package rebar.graph.aws;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.structure.Graph;

import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.TagDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.Neo4jDriver;
import rebar.util.Json;

public class ElbScanner extends AbstractEntityScanner<LoadBalancer> {

	public static final int TAG_BATCH_SIZE = 20;

	public static class RelationshipGraphOperation implements GraphOperation {

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Neo4jDriver neo4j) {

			String account = AwsScanner.class.cast(ctx).getAccount();
			String region = AwsScanner.class.cast(ctx).getRegion().getName();
			String cypher = "match (a:AwsElb {region:{region},account:{account}}),(s:AwsSubnet {region:{region},account:{account}}) where s.subnetId in a.subnets "
					+ " merge (a)-[r:RESIDES_IN]->(s) set r.graphUpdateTs=timestamp()";
			neo4j.cypher(cypher).param("region", region).param("account", account).exec();

			cypher = "match (a:AwsElb {region:{region},account:{account}})-[r]->(a:AwsSubnet) where NOT a.subnetId in a.subnets delete r";
			neo4j.cypher(cypher).param("region", region).param("account", account).exec();

			return Stream.of();
		}

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Graph g) {
			return Stream.of();
		}

	}

	public ElbScanner(AwsScanner scanner) {
		super(scanner);

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

		getGraphDB().nodes("AwsElb").idKey("name", "region", "account").withTagPrefixes(TAG_PREFIXES).properties(n)
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

		getGraphDB().nodes("AwsElb").id("arn", td.getResourceArn()).withTagPrefixes(TAG_PREFIXES).properties(data)
				.merge();

	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
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

		gc("AwsElb", ts);
		scanTags();
		getAwsScanner().getScanner(ElbTargetGroupScanner.class).scan();
		getAwsScanner().getScanner(ElbListenerScanner.class).scan();
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
				getAwsScanner().getScanner(ElbTargetGroupScanner.class)
						.scanTargetGroupByLoadBalancerArn(lb.getLoadBalancerArn());
				getAwsScanner().getScanner(ElbListenerScanner.class)
						.scanListenersByLoadBalancerArn(lb.getLoadBalancerArn());
				scanTagsByLoadBalancerArns(lb.getLoadBalancerArn());
			});
		} catch (LoadBalancerNotFoundException e) {
			getGraphDB().nodes("AwsLoadBalancer").id("name", name).id("region", getRegionName())
					.id("account", getAccount()).delete();

		}
	}

	@Override
	public void scan(JsonNode entity) {

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
			List<String> allNames = getGraphDB().nodes("AwsElb").id("account", getAccount(), "region", getRegionName())
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
}
