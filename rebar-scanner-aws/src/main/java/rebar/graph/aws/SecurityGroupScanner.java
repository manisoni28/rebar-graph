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

import java.nio.channels.ScatteringByteChannel;
import java.util.Optional;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import rebar.util.Json;

public class SecurityGroupScanner extends AwsEntityScanner<SecurityGroup, AmazonEC2Client> {

	@Override
	public void doScan() {
		scanSecurityGroups(getClient(AmazonEC2ClientBuilder.class));
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityType(entity)) {
			scanById(entity.path("groupId").asText());
		}

	}

	protected Optional<String> toArn(SecurityGroup sg) {
		return Optional.ofNullable(
				String.format("arn:aws:ec2:%s:%s:security-group/%s", getRegionName(), getAccount(), sg.getGroupId()));
	}

	public void scanById(String id) {
		checkScanArgument(id);
		try {
			DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
			request.withGroupIds(id);
			AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);
			DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(request);

			result.getSecurityGroups().forEach(sg -> {
				project(sg);
			});
		} catch (AmazonEC2Exception e) {
			if (ImmutableSet.of("InvalidGroup.NotFound").contains(e.getErrorCode())) {
				getGraphDB().nodes(AwsEntityType.AwsSecurityGroup.name())
						.id("region", getRegion().getName(), "account", getAccount(), "groupId", id).delete();

			} else {
				throw e;
			}
		}
		mergeAccountOwner();

	}

	/*
	 * public void scanByName(String name) { try { DescribeSecurityGroupsRequest
	 * request = new DescribeSecurityGroupsRequest(); request.withGroupNames(name);
	 * AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);
	 * DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(request);
	 * 
	 * result.getSecurityGroups().forEach(sg -> { projectSecurityGroup(sg); }); }
	 * catch (AmazonEC2Exception e) { if
	 * (ImmutableSet.of("InvalidGroup.NotFound").contains(e.getErrorCode())) {
	 * logger.info("removing security group: {}",name);
	 * getGraphDB().nodes().label(AwsEntities.SECURITY_GROUP_TYPE).id("account",
	 * getAccount(), "region", getRegion().getName(), "name", name).delete();
	 * 
	 * } else { throw e; } }
	 * 
	 * }
	 */

	private void scanSecurityGroups(AmazonEC2 ec2) {

		DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();

		DescribeSecurityGroupsResult result = null;
		do {
			result = ec2.describeSecurityGroups(request);

			result.getSecurityGroups().forEach(sg -> {
				tryExecute(() -> project(sg));
			});

			request = request.withNextToken(result.getNextToken());

		} while (!Strings.isNullOrEmpty(result.getNextToken()));

		mergeAccountOwner();
	}

	protected ObjectNode toJson(SecurityGroup awsObject) {

		ObjectNode n = super.toJson(awsObject);
		n.set("name", n.path("groupName"));

		n.path("tags").forEach(it -> {
			n.put(TAG_PREFIX + it.path("key").asText(), it.path("value").asText());
		});
		n.remove("tags");

		return n;
	}

	protected void project(SecurityGroup sg) {

		ObjectNode n = toJson(sg);
		n.set("name", n.path("groupName"));

		getGraphDB().nodes("AwsSecurityGroup").withTagPrefixes(TAG_PREFIXES).idKey("arn").properties(n).merge();

		if (!Strings.isNullOrEmpty(sg.getVpcId())) {
			getGraphDB().nodes("AwsVpc").id("vpcId", sg.getVpcId()).relationship("HAS").on("vpcId", "vpcId")
					.to("AwsSecurityGroup").id("arn", n.path("arn").asText()).merge();
		}

	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		scanById(id);

	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub

	}

	@Override
	protected AmazonEC2Client getClient() {
		return getClient(AmazonEC2ClientBuilder.class);
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsSecurityGroup;
	}

}
