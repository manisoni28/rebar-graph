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

import java.util.Optional;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Subnet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

public class SubnetScanner extends AwsEntityScanner<Subnet,AmazonEC2Client> {

	

	
	@Override
	protected ObjectNode toJson(Subnet awsObject) {

		ObjectNode n =  super.toJson(awsObject);
		awsObject.getTags().forEach(it->{
			n.put("tag_"+it.getKey(),it.getValue());
		});
		n.remove("tags");
		return n;
	}

	protected void project(Subnet it) {

		ObjectNode n = toJson(it);

		getGraphBuilder().nodes(getEntityTypeName()).idKey("arn").withTagPrefixes(TAG_PREFIXES).properties(n).merge();

		getGraphBuilder().nodes(AwsEntityType.AwsVpc.name()).id(VpcScanner.VPC_ID_PROPERTY, it.getVpcId()).relationship("HAS")
				.to(AwsEntityType.AwsSubnet.name()).id("subnetId", it.getSubnetId()).merge();

		getGraphBuilder().nodes("AwsSubnet").id("arn",n.path("arn").asText()).relationship("RESIDES_IN").on("availabilityZone","name").to("AwsAvailabilityZone").merge();
	}

	protected Optional<String> toArn(Subnet subnet) {
		return Optional.ofNullable(
				String.format("arn:aws:ec2:%s:%s:subnet/%s", getRegionName(), getAccount(), subnet.getSubnetId()));
	}

	public void scanById(String subnetId) {
		checkScanArgument(subnetId);
		try {
			AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);
			DescribeSubnetsResult subnetResult = ec2
					.describeSubnets(new DescribeSubnetsRequest().withSubnetIds(subnetId));

			subnetResult.getSubnets().forEach(it -> {
				project(it);
			});
		} catch (AmazonEC2Exception e) {
			if (Strings.nullToEmpty(e.getErrorCode()).equals("InvalidSubnetID.NotFound")) {

				getGraphBuilder().nodes("AwsSubnet").id("account", getAccount(), "subnetId", subnetId).delete();

			} else {
				throw e;
			}
		}
	}

	@Override
	public void doScan() {

		long ts = getGraphBuilder().getTimestamp();
		AmazonEC2Client client = getClient(AmazonEC2ClientBuilder.class);

		client.describeSubnets().getSubnets().forEach(it -> {
			// there is no paging to deal with here
			tryExecute(() -> project(it));

		});

		gc(getEntityTypeName(), ts);
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityType(entity)) {
			scanById(entity.path("subnetId").asText());
		}

	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		scanById(id);
		
	}
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsSubnet;
	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected AmazonEC2Client getClient() {
		return getClient(AmazonEC2ClientBuilder.class);
	}
}
