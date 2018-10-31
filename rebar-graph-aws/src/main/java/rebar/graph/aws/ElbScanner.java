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

import java.util.List;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.structure.Graph;

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.graph.core.GraphDB;
import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.Neo4jDriver;
import rebar.util.Json;
import rebar.util.RebarException;

public class ElbScanner extends AbstractEntityScanner<String> {

	public ElbScanner(AwsScanner scanner) {
		super(scanner);
	}

	public static class ElbRelationshipGraphOperation implements GraphOperation {

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Neo4jDriver neo4j) {

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

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Graph g) {
			throw new UnsupportedOperationException();
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

		// handle tags

		gc("AwsElb", ts);

	}

	protected String toArn(LoadBalancerDescription elb) {
		return String.format("arn:aws:elasticloadbalancing:%s:%s:loadbalancer/app/%s", getRegion().getName(),
				getAccount(), elb.getLoadBalancerName());
	}

	protected void project(LoadBalancerDescription elb) {
		ObjectNode n = toJson(elb);
		n.put("name", elb.getLoadBalancerName());
		n.put("arn", toArn(elb));

		getGraphDB().nodes("AwsElb").idKey("arn").properties(n).merge();

		List<String> instances = Lists.newArrayList();
		ArrayNode instancesNode = Json.arrayNode();
		elb.getInstances().forEach(instance -> {
			instances.add(instance.getInstanceId());
			instancesNode.add(instance.getInstanceId());
		});
		n.set("instances", instancesNode);
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

		} catch (LoadBalancerNotFoundException e) {
			getGraphDB().nodes("AwsElb").id("region", getRegionName()).id("account", getAccount()).id("name", name).delete();
		}
	}

	@Override
	public void scan(JsonNode entity) {
		assertEntityOwner(entity);

		if (isEntityType(entity, "AwsElb")) {
			String name = entity.path("name").asText();
			scanElbByName(name);

		} else {
			throw new RebarException("cannot process entity type: " + entity.path(GraphDB.ENTITY_TYPE).asText());
		}
	}

}
