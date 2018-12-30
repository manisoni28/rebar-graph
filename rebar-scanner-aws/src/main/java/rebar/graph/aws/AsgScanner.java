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
import java.util.stream.Collectors;
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
import com.google.common.collect.ImmutableList;
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
			});
			
			String cypher = "match (a:AwsAsg {arn:{arn}}),(s:AwsSubnet {account:{account}, region:{region}}) where s.subnetId in {subnets}  merge (a)-[r:LAUNCHES_INSTANCES_IN]->(s) ";

			neo4j.cypher(cypher).param("arn", n.path("arn").asText()).param("account", n.path("account").asText())
					.param("region", n.path("region").asText()).param("subnets", subnets).exec();

			
			List<String> instances = toStringList(n.path("instances"));
	
			cypher = "match (asg:AwsAsg {arn:{arn}}), (ec2:AwsEc2Instance {region:{region},account:{account}}) where ec2.instanceId in {instances} merge (asg)-[r:HAS]->(ec2)";
			neo4j.cypher(cypher).param("arn", arn).param("region",region).param("account", account).param("instances", instances).exec();
			
			cypher = "match (asg:AwsAsg {arn:{arn}})-[r]->(ec2:AwsEc2Instance) where NOT ec2.instanceId in asg.instances delete r";
			neo4j.cypher(cypher).param("arn", arn).exec();
			
			
			String launchConfigurationName = n.path("launchConfigurationName").asText();
			if (!Strings.isNullOrEmpty(launchConfigurationName)) {
				cypher = "match (a:AwsAsg {arn:{arn}}),(t:AwsLaunchConfig {account:{account}, region:{region},launchConfigurationName:{launchConfigurationName}}) merge (a)-[r:USES]->(t) set r.graphUpdateTs=timestamp()";
				neo4j.cypher(cypher).param("account", account).param("region", region)
				.param("launchConfigurationName", launchConfigurationName).param("arn", arn).exec();
				
				cypher = "match (a:AwsAsg {arn:{arn}} )-[r]->(c:AwsLaunchConfig) where r.graphUpdateTs<{ts} delete r";
				neo4j.cypher(cypher).param("arn", arn).param("ts", ts).exec();
			}

			String launchTemplateId = n.path("launchTemplateId").asText();
			if (!Strings.isNullOrEmpty(launchTemplateId)) {
				cypher = "match (a:AwsAsg {arn:{arn}}),(t:AwsLaunchTemplate {account:{account}, region:{region},launchTemplateId:{launchTemplateId}}) merge (a)-[r:USES]->(t) set r.graphUpdateTs=timestamp()";
				neo4j.cypher(cypher).param("account", account).param("region", region)
						.param("launchTemplateId", launchTemplateId).param("arn", arn).exec();
				
				cypher = "match (a:AwsAsg {arn:{arn}} )-[r]->(t:AwsLaunchTemplate) where r.graphUpdateTs<{ts} delete r";
				neo4j.cypher(cypher).param("arn", arn).param("ts", ts).exec();
			}


			List<String> loadBalancerNames = null;
			JsonNode lbn = n.path("loadBalancerNames");
			if (lbn.isArray()) {
				loadBalancerNames = Json.objectMapper().convertValue(lbn, List.class);
			}
			else {
				loadBalancerNames = ImmutableList.of();
			}
			
			// establish relationship between ELB->ASG
			cypher = "match (elb:AwsElb {region:{region},account:{account}}),(asg:AwsAsg {arn:{arn}}) where elb.name in asg.loadBalancerNames merge (elb)-[r:ATTACHED_TO]->(asg) set r.graphUpdateTs=timestamp()";
			neo4j.cypher(cypher).param("arn", arn).param("region", region).param("account", account).param("names", loadBalancerNames).exec();
			
			// delete old ELB->ASG
			cypher = "match (elb:AwsElb)-[r:ATTACHED_TO]->(asg:AwsAsg {arn:{arn}}) where NOT (elb.name in asg.loadBalancerNames) delete r";
			neo4j.cypher(cypher).param("arn", arn).exec();
			
			return Stream.of();
		}

		@Override
		public Stream<JsonNode> exec(Scanner ctx, JsonNode n, Graph g) {
			return Stream.of();
		}

	}



	protected void project(AutoScalingGroup asg) {

		ObjectNode n = toJson(asg);

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
		
		asg.getTags().forEach(tag->{
			n.put(TAG_PREFIX+tag.getKey(),tag.getValue());
		});
		n.remove("tags");
		
		getGraphDB().nodes(AwsEntities.ASG_TYPE).withTagPrefixes(TAG_PREFIXES).idKey("arn").properties(n).merge();

		
	
		getAwsScanner().execGraphOperation(AsgScanner.AsgRelationshipGraphOperation.class, n);
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

	public static List<String> toStringList(JsonNode n) {
		if (n==null || !n.isArray()) {
			return ImmutableList.of();
		}
		List<String> list = Lists.newArrayList();
		n.forEach(it->{
				list.add(it.asText(null));
		});
		return list;
	}
}
