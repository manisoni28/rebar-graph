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

import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.LaunchTemplateSpecification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.graph.core.GraphDB;
import rebar.util.Json;
import rebar.util.RebarException;

public class AsgScanner extends AbstractEntityScanner<AutoScalingGroup> {

	org.slf4j.Logger logger = LoggerFactory.getLogger(AsgScanner.class);

	public AsgScanner(AwsScanner scanner) {
		super(scanner);

	}

	protected void projectRelationship(AutoScalingGroup asg, com.amazonaws.services.autoscaling.model.Instance instance) {

		getGraphDB().nodes("AwsEc2Instance").id("region", getRegionName()).id("account", getAccount())
				.id("instanceId", instance.getInstanceId()).property("autoScalingGroupName", asg.getAutoScalingGroupName()).merge();
		
	}

	protected void project(AutoScalingGroup asg) {

		ObjectNode n = toJson(asg);

		getGraphDB().nodes(AwsEntities.ASG_TYPE).idKey("arn").properties(n).merge();

		asg.getInstances().forEach(instance -> {
			projectRelationship(asg, instance);
		});
	}

	protected ObjectNode toJson(AutoScalingGroup asg) {

		ObjectNode n = super.toJson(asg);
		n.set("name", n.path("autoScalingGroupName"));
		n.remove("launchTemplate");
		ArrayNode instanceIdList = Json.objectMapper().createArrayNode();
		asg.getInstances().forEach(instance->{
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
		  getGraphDB().nodes("AwsAsg").id("region",getRegionName()).id("account",
		  getAccount()).relationship("HAS").on("autoScalingGroupName",
		  "autoScalingGroupName") .on("account", "account").on("region",
		  "region").to("AwsEc2Instance").id("region",getRegionName()).id("account",
		  getAccount()).merge();
		 
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
