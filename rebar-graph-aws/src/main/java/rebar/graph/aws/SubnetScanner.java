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

public class SubnetScanner extends AbstractEntityScanner<Subnet> {

	public SubnetScanner(AwsScanner scanner) {
		super(scanner);
	}

	private void projectSubnet(Subnet it) {

		ObjectNode n = toJson(it);

		getGraphDB().nodes().label(AwsEntities.SUBNET_TYPE).idKey("arn").properties(n).merge();

		getGraphDB().nodes(AwsEntities.VPC_TYPE).id(VpcScanner.VPC_ID_PROPERTY, it.getVpcId()).relationship("HAS")
				.to("AwsSubnet").id("subnetId", it.getSubnetId()).merge();

	}

	protected Optional<String> toArn(Subnet subnet) {
		return Optional.ofNullable(
				String.format("arn:aws:ec2:%s:%s:subnet/%s", getRegionName(), getAccount(), subnet.getSubnetId()));
	}

	public void scanById(String subnetId) {
		try {
			AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);
			DescribeSubnetsResult subnetResult = ec2
					.describeSubnets(new DescribeSubnetsRequest().withSubnetIds(subnetId));

			subnetResult.getSubnets().forEach(it -> {
				projectSubnet(it);
			});
		} catch (AmazonEC2Exception e) {
			if (Strings.nullToEmpty(e.getErrorCode()).equals("InvalidSubnetID.NotFound")) {

				getGraphDB().nodes().label("AwsSubnet").id("account", getAccount(), "subnetId", subnetId).delete();

			} else {
				throw e;
			}
		}
	}

	@Override
	public void doScan() {

		long ts = getGraphDB().getTimestamp();
		AmazonEC2Client client = getClient(AmazonEC2ClientBuilder.class);

		client.describeSubnets().getSubnets().forEach(it -> {
			// there is no paging to deal with here
			tryExecute(() -> projectSubnet(it));

		});

		gc(getEntityType(), ts);
	}

	@Override
	public void scan(JsonNode entity) {
		if (isEntityType(entity)) {
			scanById(entity.path("subnetId").asText());
		}

	}

}
