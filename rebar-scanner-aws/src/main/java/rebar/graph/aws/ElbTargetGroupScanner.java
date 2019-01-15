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

import java.util.stream.Stream;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.Neo4jDriver;
import rebar.util.Json;

public class ElbTargetGroupScanner extends AbstractEntityScanner<TargetGroup> {

	public static class TargetGroupGraphOperation implements GraphOperation {

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Neo4jDriver neo4j) {

		//	String targetGroupArn = n.path("arn").asText();

			String account = AwsScanner.class.cast(ctx).getAccount();
			String region = AwsScanner.class.cast(ctx).getRegion().getName();

			//if (!Strings.isNullOrEmpty(targetGroupArn)) {
				String cypher = "match (a:AwsElbTargetGroup ),(lb:AwsElb {region:{region},account:{account}}) where lb.arn in a.loadBalancerArns  merge (lb)-[r:DISTRIBUTES_TRAFFIC_TO]->(a) set r.graphUpdateTs=timestamp()";
				neo4j.cypher(cypher).param("region", region).param("account", account)
						.exec();

				cypher = "match (x:AwsElb {region:{region},account:{account}})-[r]->(tg:AwsElbTargetGroup ) where NOT x.arn in tg.targetGroupArns delete r";
				neo4j.cypher(cypher).param("region", region).param("account", account)
						.exec();

	
			
		
		
				cypher = "match (a:AwsAsg ),(g:AwsElbTargetGroup ) where g.arn in a.targetGroupARNs merge (g)-[r:ATTACHED_TO]->(a) set r.graphUpdateTs=timestamp()";
				neo4j.cypher(cypher).param("region", region).param("account", account).exec();
				
				cypher = "match (g:AwsElbTargetGroup)-[r:ATTACHED_TO]->(a:AwsAsg {region:{region},account:{account}}) where NOT g.arn in a.targetGroupARNs delete r";
				neo4j.cypher(cypher).param("region", region).param("account", account).exec();

			
			return Stream.of();
		}

	

	}

	public ElbTargetGroupScanner(AwsScanner scanner) {
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
			result.getTargetGroups().forEach(tg -> {
				project(tg);
			});
			tgRequest.setMarker(result.getNextMarker());
		} while (!Strings.isNullOrEmpty(tgRequest.getMarker()));

		getAwsScanner().execGraphOperation(TargetGroupGraphOperation.class, Json.objectNode());
		gc("AwsElbTargetGroup", ts);
	}

	protected void project(com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup tg) {
		ObjectNode n = toJson(tg);
		n.put("name", n.path("targetGroupName").asText());
		n.put("arn", tg.getTargetGroupArn());
		
		getGraphDB().nodes("AwsElbTargetGroup").idKey("name", "region", "account").properties(n).merge();
		getAwsScanner().execGraphOperation(TargetGroupGraphOperation.class, n);
	}

	public void scanTargetGroupByLoadBalancerArn(String arn) {
		AmazonElasticLoadBalancingClient client = getAwsScanner()
				.getClient(AmazonElasticLoadBalancingClientBuilder.class);

		DescribeTargetGroupsRequest tgRequest = new DescribeTargetGroupsRequest().withLoadBalancerArn(arn);

		DescribeTargetGroupsResult result = client.describeTargetGroups(tgRequest);

		result.getTargetGroups().forEach(it -> {
			project(it);
		});
	}

	public void scanTargetGroupByName(String name) {
		AmazonElasticLoadBalancingClient client = getAwsScanner()
				.getClient(AmazonElasticLoadBalancingClientBuilder.class);

		
		DescribeTargetGroupsRequest tgRequest = new DescribeTargetGroupsRequest().withNames(name);

		DescribeTargetGroupsResult result = client.describeTargetGroups(tgRequest);

		result.getTargetGroups().forEach(it -> {
			project(it);
		});
	}

	@Override
	public void scan(JsonNode entity) {
		String name = entity.path("name").asText();
		scanTargetGroupByName(name);

	}

	@Override
	public void scan(String id) {
		scanTargetGroupByName(id);	
	}

}
