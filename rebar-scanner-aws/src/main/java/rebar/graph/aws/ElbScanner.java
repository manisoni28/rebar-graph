/**
 * Copyright 2018-2019 Rob Schoening
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rebar.graph.aws;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.TagDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.GraphDriver;
import rebar.util.Json;

public class ElbScanner extends AwsEntityScanner<LoadBalancer,AmazonElasticLoadBalancingClient> {

	public static final int TAG_BATCH_SIZE = 20;

	public static class RelationshipGraphOperation implements GraphOperation {

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, GraphDriver neo4j) {

			String account = AwsScanner.class.cast(ctx).getAccount();
			String region = AwsScanner.class.cast(ctx).getRegion().getName();
			String cypher = "match (a:AwsElb {region:{region},account:{account}}),(s:AwsSubnet {region:{region},account:{account}}) where s.subnetId in a.subnets "
					+ " merge (a)-[r:RESIDES_IN]->(s) set r.graphUpdateTs=timestamp()";
			neo4j.cypher(cypher).param("region", region).param("account", account).exec();

			cypher = "match (a:AwsElb {region:{region},account:{account}})-[r]->(a:AwsSubnet) where NOT a.subnetId in a.subnets delete r";
			neo4j.cypher(cypher).param("region", region).param("account", account).exec();

			return Stream.of();
		}

	

	}

	

	protected ObjectNode toJson(LoadBalancer lb) {
		ObjectNode n = super.toJson(lb);
		n.put("name", lb.getLoadBalancerName());
		n.put("arn", lb.getLoadBalancerArn());
		n.put("stateCode", lb.getState() != null ? lb.getState().getCode() : null);
		n.put("stateReason", lb.getState() != null ? lb.getState().getReason() : null);
		n.remove("state");
		ArrayNode zones = Json.arrayNode();
		ArrayNode subnets = Json.arrayNode();
		n.path("availabilityZones").forEach(az -> {
			zones.add(az.path("zoneName").asText());
			subnets.add(az.path("subnetId").asText());
			// there is other nested information that (loadBalancerAddresses) that we may
			// want to pull out
		});
		n.set("availabilityZones", zones);
		n.set("subnets", subnets);
		return n;
	}

	protected void project(LoadBalancer lb) {
		ObjectNode n = toJson(lb);
		
		getGraphBuilder().nodes("AwsElb").idKey("name", "region", "account","type").withTagPrefixes(TAG_PREFIXES).properties(n)
				.merge();

		getAwsScanner().execGraphOperation(RelationshipGraphOperation.class, n);

	}

	protected ObjectNode toJson(TagDescription td) {
		ObjectNode data = Json.objectNode();

		td.getTags().forEach(it -> {
			data.put(TAG_PREFIX + it.getKey(), it.getValue());
		});
		return data;
	}

	protected void project(TagDescription td) {

		ObjectNode data = toJson(td);

		getGraphBuilder().nodes("AwsElb").id("arn", td.getResourceArn()).withTagPrefixes(TAG_PREFIXES).properties(data)
				.merge();

	}

	@Override
	protected void doScan() {

		long ts = getGraphBuilder().getTimestamp();
		AmazonElasticLoadBalancingClient client = getAwsScanner()
				.getClient(AmazonElasticLoadBalancingClientBuilder.class);

		DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
		do {
			DescribeLoadBalancersResult result = client.describeLoadBalancers(request);
			result.getLoadBalancers().forEach(lb -> {
				project(lb);
			});
			request.setMarker(result.getNextMarker());
		} while (!Strings.isNullOrEmpty(request.getMarker()));

		gc("AwsElb", ts,"type","network");
		gc("AwsElb", ts,"type","application");
		scanTags();
		getAwsScanner().getEntityScanner(ElbTargetGroupScanner.class).scan();
		getAwsScanner().getEntityScanner(ElbListenerScanner.class).scan();
	}

	public void scanLoadBalancerByName(String name) {
		try {
			AmazonElasticLoadBalancingClient client = getAwsScanner()
					.getClient(AmazonElasticLoadBalancingClientBuilder.class);

			DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
			request.withNames(name);
			DescribeLoadBalancersResult result = client.describeLoadBalancers(request);
			result.getLoadBalancers().forEach(lb -> {
				project(lb);
				getAwsScanner().getEntityScanner(ElbTargetGroupScanner.class)
						.scanTargetGroupByLoadBalancerArn(lb.getLoadBalancerArn());
				getAwsScanner().getEntityScanner(ElbListenerScanner.class)
						.scanListenersByLoadBalancerArn(lb.getLoadBalancerArn());
				scanTagsByLoadBalancerArns(lb.getLoadBalancerArn());
			});
		} catch (LoadBalancerNotFoundException e) {
			// important to NOT delete classic
			getGraphBuilder().nodes("AwsLoadBalancer").id("name", name).id("region", getRegionName())
					.id("account", getAccount()).id("type","network").delete();
			getGraphBuilder().nodes("AwsLoadBalancer").id("name", name).id("region", getRegionName())
			.id("account", getAccount()).id("type","application").delete();
		}
	}

	@Override
	public void doScan(JsonNode entity) {

		assertEntityOwner(entity);

		String type = entity.path("type").asText();
		if (isEntityType(entity, "AwsElb") && (type.equals("network") || type.equals("application"))) {
			String name = entity.path("name").asText();
			scanLoadBalancerByName(name);
		}

	}

	public void scanTags() {
		scanTagsByLoadBalancerArns();
	}

	public void scanTagsByLoadBalancerArns(List<String> arnList) {
		if (arnList == null) {
			arnList = ImmutableList.of();
		}
		scanTagsByLoadBalancerArns(arnList.toArray(new String[arnList.size()]));
	}

	public void scanTagsByLoadBalancerArns(String... arns) {

		if (arns != null && arns.length > TAG_BATCH_SIZE) {
			ElbClassicScanner.batch(Arrays.asList(arns), TAG_BATCH_SIZE).forEach(microbatch -> {
				scanTagsByLoadBalancerArns(microbatch.toArray(new String[0]));
			});
			return;
		}

		if (arns == null || arns.length == 0) {
			Set<String> types = ImmutableSet.of("network", "application");
			List<String> allNames = getGraphBuilder().nodes("AwsElb").id("account", getAccount(), "region", getRegionName())
					.match().filter(n -> types.contains(n.path("type").asText())).map(n -> n.path("arn").asText())
					.collect(Collectors.toList());
			if (allNames.isEmpty()) {
				return;
			} else {
				scanTagsByLoadBalancerArns(allNames.toArray(new String[allNames.size()]));
				return;
			}
		}

		com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsRequest request = new DescribeTagsRequest();
		request = request.withResourceArns(arns);

		AmazonElasticLoadBalancingClient client = getClient(AmazonElasticLoadBalancingClientBuilder.class);

		com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsResult result = client.describeTags(request);

		result.getTagDescriptions().forEach(it -> {
			project(it);
		});

	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		scanLoadBalancerByName(id);
		
	}
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsElb;
	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected AmazonElasticLoadBalancingClient getClient() {
		return getClient(AmazonElasticLoadBalancingClientBuilder.class);
	}
}
