/**
 * Copyright 2018 Rob Schoening
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerNotFoundException;
import com.amazonaws.services.elasticloadbalancing.model.TagDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.machinezoo.noexception.Exceptions;

import rebar.graph.core.GraphDB;
import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.GraphDriver;
import rebar.util.Json;

public class ElbClassicScanner extends AbstractEntityScanner<LoadBalancerDescription> {

	public static final int TAG_BATCH_SIZE=20;
	public ElbClassicScanner(AwsScanner scanner) {
		super(scanner);
	}

	public static class ElbRelationshipGraphOperation implements GraphOperation {

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, GraphDriver neo4j) {

			String arn = n.path("arn").asText();
			String region = n.path("region").asText();
			String account = n.path("account").asText();
			String cypher = "match (elb:AwsElb {arn:{arn}}),(ec2:AwsEc2Instance {region:{region},account:{account}}) where ec2.instanceId in elb.instances merge (elb)-[r:DISTRIBUTES_TRAFFIC_TO]->(ec2) set r.graphUpdateTs=timestamp()";

			neo4j.cypher(cypher).param("arn", arn).param("account", account).param("region", region).exec();

			cypher = "match (elb:AwsElb {arn:{arn}})-[r]->(ec2) where NOT ec2.instanceId in elb.instances delete r";
			neo4j.cypher(cypher).param("arn", arn).param("account", account).param("region", region).exec();

			// ELB -> AwsSubnet

			cypher = "match (elb:AwsElb {arn:{arn}}),(subnet:AwsSubnet {region:{region},account:{account}}) where subnet.subnetId in elb.subnets merge (elb)-[r:RESIDES_IN]->(subnet) set r.graphUpdateTs=timestamp()";
			neo4j.cypher(cypher).param("arn", arn).param("account", account).param("region", region).exec();

			cypher = "match (elb:AwsElb {arn:{arn}})-[r:RESIDES_IN]->(subnet:AwsSubnet) where NOT subnet.subnetId in elb.subnets delete r";
			neo4j.cypher(cypher).param("arn", arn).param("account", account).param("region", region).exec();

			return Stream.of();

		}

	

	}

	@Override
	protected void doScan() {

		scanElbByName("foo");
		long ts = System.currentTimeMillis();

		AmazonElasticLoadBalancingClient client = getClient(AmazonElasticLoadBalancingClientBuilder.class);

		DescribeLoadBalancersRequest request = new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest();

		do {
			DescribeLoadBalancersResult result = client.describeLoadBalancers(request);

			String nextMarker = result.getNextMarker();

			result.getLoadBalancerDescriptions().iterator().forEachRemaining(elb -> {

				tryExecute(() -> project(elb));
			});

			request.setMarker(nextMarker);
		} while (!Strings.isNullOrEmpty(request.getMarker()));

		scanTags();

		// The extra qualification on GC is important
		gc("AwsElb", ts,"type","classic");

	}

	protected Optional<String> toArn(LoadBalancerDescription elb) {

		//https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
		return Optional.of(String.format("arn:aws:elasticloadbalancing:%s:%s:loadbalancer/%s",
				getRegion().getName(), getAccount(), elb.getLoadBalancerName()));
	}

	protected ObjectNode toJson(LoadBalancerDescription elb) {
		ObjectNode n = super.toJson(elb);
		
		n.put("name", elb.getLoadBalancerName());
		n.put("arn", toArn(elb).get());
		n.put("type", "classic");

		List<String> instances = Lists.newArrayList();
		ArrayNode instancesNode = Json.arrayNode();
		elb.getInstances().forEach(instance -> {
			instances.add(instance.getInstanceId());
			instancesNode.add(instance.getInstanceId());
		});
		n.set("instances", instancesNode);
		return n;
	}
	protected void project(LoadBalancerDescription elb) {
		ObjectNode n = toJson(elb);
		
		getGraphDB().nodes("AwsElb").idKey("arn").properties(n).merge();

		getAwsScanner().execGraphOperation(ElbRelationshipGraphOperation.class, n);

	}

	protected void scanElbByName(String name) {

		try {
			if (Strings.isNullOrEmpty(name)) {
				return;
			}
			AmazonElasticLoadBalancingClient client = getClient(AmazonElasticLoadBalancingClientBuilder.class);
			DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
			request.withLoadBalancerNames(name);
			DescribeLoadBalancersResult result = client.describeLoadBalancers(request);
			result.getLoadBalancerDescriptions().forEach(it -> {
				project(it);
			});
			
			scanTagsByLoadBalancerNames(name);

		} catch (LoadBalancerNotFoundException e) {
			getGraphDB().nodes("AwsElb").id("region", getRegionName()).id("account", getAccount()).id("name", name)
					.delete();
		}
	}

	@Override
	public void scan(JsonNode entity) {
		assertEntityOwner(entity);

		if (isEntityType(entity, "AwsElb") && "classic".equals(entity.path("type").asText())) {
			String name = entity.path("name").asText();
			scanElbByName(name);

		}
	}

	protected void project(TagDescription td) {

		ObjectNode data = Json.objectNode();

	
		td.getTags().forEach(it -> {
			data.put(TAG_PREFIX + it.getKey(), it.getValue());
		});

		getGraphDB().nodes("AwsElb").id("type","classic").id("account", getAccount()).id("region", getRegion().getName()).id("name", td.getLoadBalancerName())
				.withTagPrefixes(TAG_PREFIXES).properties(data).merge();

	}

	public void scanTags() {
		scanTagsByLoadBalancerNames();
	}
	
	
	public static List<List<String>> batch(List<String> list, int batchSize) {
		List<List<String>> listOfLists = Lists.newArrayList();
		List<String> currentList = Lists.newArrayList();
		listOfLists.add(currentList);
		for (String it: list) {
			currentList.add(it);
			if (currentList.size()==batchSize) {
				currentList = Lists.newArrayList();
				listOfLists.add(currentList);
			}
		}
		return listOfLists;
	}
	
	public void scanTagsByLoadBalancerNames(List<String> names) {
		if (names==null) {
			names = ImmutableList.of();
		}
		scanTagsByLoadBalancerNames(names.toArray(new String[names.size()]));
	}
	public void scanTagsByLoadBalancerNames(String ...names) {
		
		if (names!=null && names.length>TAG_BATCH_SIZE) {
			batch(Arrays.asList(names),TAG_BATCH_SIZE).forEach(microbatch->{
				scanTagsByLoadBalancerNames(microbatch.toArray(new String[0]));
			});
			return;
		}
		
	
		if (names==null || names.length==0) {
			List<String> allNames = getGraphDB().nodes("AwsElb").id("account",getAccount(),"region",getRegionName()).id("type","classic").match().map(n->n.path("name").asText()).collect(Collectors.toList());
			if (allNames.isEmpty()) {
				return;
			}
			else {
				scanTagsByLoadBalancerNames(allNames.toArray(new String[allNames.size()]));
				return;
			}
		}

		DescribeTagsRequest request = new DescribeTagsRequest();
			request = request.withLoadBalancerNames(names);
		
		AmazonElasticLoadBalancingClient client = getClient(AmazonElasticLoadBalancingClientBuilder.class);
	
		com.amazonaws.services.elasticloadbalancing.model.DescribeTagsResult result = client.describeTags(request);
		Map<String, Map<String, String>> lbTags = Maps.newHashMap();
		result.getTagDescriptions().forEach(it -> {
			project(it);
		});

	}

	@Override
	public String getEntityType() {
		return "AwsElb";
	}

	@Override
	public void scan(String id) {
		scanElbByName(id);
		
	}


}
