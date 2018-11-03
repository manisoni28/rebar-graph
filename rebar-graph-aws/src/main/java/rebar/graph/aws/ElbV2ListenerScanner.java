package rebar.graph.aws;

import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.structure.Graph;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
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

public class ElbV2ListenerScanner extends AbstractEntityScanner<Listener> {

	public static class RelationshipGraphOperation implements GraphOperation {

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Neo4jDriver neo4j) {
			
			String loadBalancerArn = n.path("loadBalancerArn").asText().trim();
			long ts = ctx.getRebarGraph().getGraphDB().getTimestamp();
			String cypher = "match (a:AwsLoadBalancer {arn:{arn}}),(x:AwsLoadBalancerListener {loadBalancerArn:{arn}}) merge (a)-[r:HAS]->(x) set r.graphUpdateTs=timestamp()";
	
			neo4j.cypher(cypher).param("arn", loadBalancerArn).exec();
				
			
			cypher = "match (a:AwsLoadBalancer {arn:{arn}})-[r]->(x:AwsLoadBalancerListener) where r.graphUpdateTs<{ts} delete r,x";
			neo4j.cypher(cypher).param("arn", loadBalancerArn).param("ts", ts).exec();
			
			
			return Stream.of();
		}

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Graph g) {
			return Stream.of();
		}

	}

	public ElbV2ListenerScanner(AwsScanner scanner) {
		super(scanner);
	}

	@Override
	protected void doScan() {
		long ts = getGraphDB().getTimestamp();
		
		getGraphDB().nodes("AwsLoadBalancer").id("region",getRegionName()).id("account",getAccount()).match().map(n->n.path("arn").asText()).forEach(arn->{
			scanListenerByLoadBalancerArn(arn);
		});
		
		gc("AwsLoadBalancerListener", ts);
	}

	public void scanListenerByLoadBalancerArn(String arn) {
		AmazonElasticLoadBalancingClient client = getAwsScanner()
				.getClient(AmazonElasticLoadBalancingClientBuilder.class);

		DescribeListenersRequest request = new DescribeListenersRequest();

		request.withLoadBalancerArn(arn);
		do {
			DescribeListenersResult result = client.describeListeners(request);
			result.getListeners().forEach(listener -> {
				project(listener);
			});
			request.setMarker(result.getNextMarker());
		} while (!Strings.isNullOrEmpty(request.getMarker()));
		
		
		getAwsScanner().execGraphOperation(RelationshipGraphOperation.class, Json.objectNode().put("loadBalancerArn", arn));
	}

	protected void project(Listener listener) {
		ObjectNode n = toJson(listener);

		n.put("arn", listener.getListenerArn());
	

		getGraphDB().nodes("AwsLoadBalancerListener").idKey("arn").properties(n).merge();
		
	}

	@Override
	public void scan(JsonNode entity) {
		String arn = entity.path("arn").asText();
		AmazonElasticLoadBalancingClient client = getAwsScanner()
				.getClient(AmazonElasticLoadBalancingClientBuilder.class);

		DescribeListenersRequest request = new DescribeListenersRequest().withListenerArns(arn);

		DescribeListenersResult result = client.describeListeners(request);

		result.getListeners().forEach(it -> {
			project(it);
		});

	}

}
