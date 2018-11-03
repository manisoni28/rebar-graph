package rebar.graph.aws;

import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.structure.Graph;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.Neo4jDriver;
import rebar.util.Json;

public class ElbV2TargetGroupScanner extends AbstractEntityScanner<TargetGroup> {

	public static class TargetGroupGraphOperation implements GraphOperation {

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Neo4jDriver neo4j) {
			
			String targetGroupArn = n.path("arn").asText();
		
			String account = n.path("account").asText();
			String region = n.path("region").asText();
		
			String cypher = "match (a:AwsLoadBalancerTargetGroup ),(lb:AwsLoadBalancer {region:{region},account:{account}}) where lb.arn in a.loadBalancerArns  merge (lb)-[r:DISTRIBUTES_TRAFFIC_TO]->(a) set r.graphUpdateTs=timestamp()";
			neo4j.cypher(cypher).param("arn", targetGroupArn).param("region",region).param("account",account).exec();
			
			cypher = "match (x:AwsLoadBalancer {region:{region},account:{account}})-[r]->(tg:AwsLoadBalancerTargetGroup {arn:{arn}}) where NOT x.arn in tg.targetGroupArns delete r";
			neo4j.cypher(cypher).param("arn", targetGroupArn).param("region",region).param("account",account).exec();
			return Stream.of();
		}

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Graph g) {
			return Stream.of();
		}
		
	}
	public ElbV2TargetGroupScanner(AwsScanner scanner) {
		super(scanner);
	}
	@Override
	protected void doScan() {
		long ts = getGraphDB().getTimestamp();
		AmazonElasticLoadBalancingClient client = getAwsScanner()
				.getClient(AmazonElasticLoadBalancingClientBuilder.class);
		
		DescribeTargetGroupsRequest tgRequest = new DescribeTargetGroupsRequest();
		do {
			DescribeTargetGroupsResult result = client.describeTargetGroups(tgRequest);
			result.getTargetGroups().forEach(tg->{
				project(tg);
			});
			tgRequest.setMarker(result.getNextMarker());
		}
		while (!Strings.isNullOrEmpty(tgRequest.getMarker()));
		
		
		gc("AwsLoadBalancerTargetGroup",ts);
	}

	protected void project(com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup tg) {
		ObjectNode n = toJson(tg);
		n.put("name",n.path("targetGroupName").asText());
		n.put("arn", tg.getTargetGroupArn());
		
	
		getGraphDB().nodes("AwsLoadBalancerTargetGroup").idKey("name","region","account").properties(n).merge();
		getAwsScanner().execGraphOperation(TargetGroupGraphOperation.class,n);
	}
	@Override
	public void scan(JsonNode entity) {
		String name = entity.path("name").asText();
		AmazonElasticLoadBalancingClient client = getAwsScanner()
				.getClient(AmazonElasticLoadBalancingClientBuilder.class);
		
		DescribeTargetGroupsRequest tgRequest = new DescribeTargetGroupsRequest().withNames(name);
		
		DescribeTargetGroupsResult result = client.describeTargetGroups(tgRequest);
		
		result.getTargetGroups().forEach(it->{
			project(it);
		});
		
	}

}
