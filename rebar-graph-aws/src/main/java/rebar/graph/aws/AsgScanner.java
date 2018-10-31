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
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.LaunchTemplateSpecification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.graph.core.GraphDB;
import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.Neo4jDriver;
import rebar.util.Json;
import rebar.util.RebarException;

public class AsgScanner extends AbstractEntityScanner<AutoScalingGroup> {

	org.slf4j.Logger logger = LoggerFactory.getLogger(AsgScanner.class);

	public AsgScanner(AwsScanner scanner) {
		super(scanner);

	}

	public static class AsgRelationshipGraphOperation implements GraphOperation {

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Neo4jDriver neo4j) {

			
			long ts = ctx.getRebarGraph().getGraphDB().getTimestamp();
			
			String region = n.path("region").asText();
			String account = n.path("account").asText();
			String arn = n.path("arn").asText();
			
			
			List<String> subnets = Lists.newArrayList();
			n.path("vpcZoneIdentifier").forEach(it->{
				subnets.add(it.asText());
			} );
			
		

			String cypher = "match (a:AwsAsg {arn:{arn}}),(s:AwsSubnet {account:{account}, region:{region}}) where s.subnetId in {subnets}  merge (a)-[r:LAUNCHES_INSTANCES_IN]->(s) ";

			neo4j.cypher(cypher).param("arn", n.path("arn").asText()).param("account", n.path("account").asText())
					.param("region", n.path("region").asText()).param("subnets", subnets).exec();

			
	
			
			String launchConfigurationName = n.path("launchConfigurationName").asText();
			if (!Strings.isNullOrEmpty(launchConfigurationName)) {
				cypher = "match (a:AwsAsg {arn:{arn}}),(t:AwsLaunchConfig {account:{account}, region:{region},launchConfigurationName:{launchConfigurationName}}) merge (a)-[r:USES]->(t) set r.graphUpdateTs=timestamp()";
				neo4j.cypher(cypher).param("account", account).param("region", region)
				.param("launchConfigurationName", launchConfigurationName).param("arn", arn).exec();
				
				cypher = "match (a:AwsAsg {arn:{arn}} )-[r]->(AwsLaunchConfig) where r.graphUpdateTs<{ts} delete r";
				neo4j.cypher(cypher).param("arn", arn).param("ts", ts).exec();
			}

			String launchTemplateId = n.path("launchTemplateId").asText();
			if (!Strings.isNullOrEmpty(launchTemplateId)) {
				cypher = "match (a:AwsAsg {arn:{arn}}),(t:AwsLaunchTemplate {account:{account}, region:{region},launchTemplateId:{launchTemplateId}}) merge (a)-[r:USES]->(t) set r.graphUpdateTs=timestamp()";
				neo4j.cypher(cypher).param("account", account).param("region", region)
						.param("launchTemplateId", launchTemplateId).param("arn", arn).exec();
				
				cypher = "match (a:AwsAsg {arn:{arn}} )-[r]->(AwsLaunchTemplate) where r.graphUpdateTs<{ts} delete r";
				neo4j.cypher(cypher).param("arn", arn).param("ts", ts).exec();
			}

			
			
		

			return Stream.of();
		}

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Graph g) {
			return Stream.of();
		}

	}

	protected void projectRelationship(AutoScalingGroup asg,
			com.amazonaws.services.autoscaling.model.Instance instance) {

		getGraphDB().nodes("AwsEc2Instance").id("region", getRegionName()).id("account", getAccount())
				.id("instanceId", instance.getInstanceId())
				.property("autoScalingGroupName", asg.getAutoScalingGroupName()).merge();

	}

	protected void project(AutoScalingGroup asg) {

		ObjectNode n = toJson(asg);

		getGraphDB().nodes(AwsEntities.ASG_TYPE).idKey("arn").properties(n).merge();

		asg.getInstances().forEach(instance -> {
			projectRelationship(asg, instance);
		});

		getAwsScanner().execGraphOperation(AsgScanner.AsgRelationshipGraphOperation.class, n);
	}

	protected ObjectNode toJson(AutoScalingGroup asg) {

		ObjectNode n = super.toJson(asg);
		n.set("name", n.path("autoScalingGroupName"));
		n.remove("launchTemplate");
		n.put("vpcZoneIdentifier", asg.getVPCZoneIdentifier());
		n.set("vpcZoneIdentifier",Json.objectMapper().valueToTree(Splitter.on(",").omitEmptyStrings().trimResults().splitToList(asg.getVPCZoneIdentifier())));
		
		n.remove("vpczoneIdentifier");
		ArrayNode instanceIdList = Json.objectMapper().createArrayNode();
		asg.getInstances().forEach(instance -> {
			instanceIdList.add(instance.getInstanceId());
		});
		n.set("instances", instanceIdList);
		LaunchTemplateSpecification lt = asg.getLaunchTemplate();
		if (lt != null) {
			n.put("launchTemplateId", lt.getLaunchTemplateId()).put("launchTemplateName", lt.getLaunchTemplateName())
					.put("launchTemplateVersion", lt.getVersion());
		} else {
			n.put("launchTemplateId", (String) null).put("launchTemplateName", (String) null)
					.put("launchTemplateVersion", (String) null);
		}

		return n;

	}

	protected Optional<String> toArn(AutoScalingGroup awsEntity) {
		return Optional.ofNullable(awsEntity.getAutoScalingGroupARN());
	}

	public void scanByName(String name) {

		AmazonAutoScalingClient client = getClient(AmazonAutoScalingClientBuilder.class);

		DescribeAutoScalingGroupsRequest r = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(name);

		DescribeAutoScalingGroupsResult result = client.describeAutoScalingGroups(r);

		if (result.getAutoScalingGroups().isEmpty()) {
			getGraphDB().nodes().label(AwsEntities.ASG_TYPE)
					.id("name", name, "region", getRegionName(), "account", getAccount()).delete();
		} else {
			result.getAutoScalingGroups().forEach(it -> {
				project(it);
			});
		}

	}

	public void doScan() {

		long ts = getGraphDB().getTimestamp();
		AmazonAutoScalingClient client = getClient(AmazonAutoScalingClientBuilder.class);

		DescribeAutoScalingGroupsRequest r = new DescribeAutoScalingGroupsRequest();

		do {
			DescribeAutoScalingGroupsResult result = client.describeAutoScalingGroups(r);

			result.getAutoScalingGroups().forEach(asg -> {
				project(asg);

			});
			r.setNextToken(result.getNextToken());

		} while (!Strings.isNullOrEmpty(r.getNextToken()));
		gc(AwsEntities.ASG_TYPE, ts);

		// connect AwsAsg->AwsEc2Instance
		getGraphDB().nodes("AwsAsg").id("region", getRegionName()).id("account", getAccount()).relationship("HAS")
				.on("autoScalingGroupName", "autoScalingGroupName").on("account", "account").on("region", "region")
				.to("AwsEc2Instance").id("region", getRegionName()).id("account", getAccount()).merge();

	}

	@Override
	public void scan(JsonNode entity) {
		assertEntityOwner(entity);

		if (isEntityType(entity, AwsEntities.ASG_TYPE)) {
			scanByName(entity.path("name").asText());
		} else {
			throw new RebarException("cannot handle entityType: " + entity.path(GraphDB.ENTITY_TYPE).asText());
		}

	}

}
