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
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.util.RebarException;

public class VpcScanner extends AwsEntityScanner<Vpc, AmazonEC2Client> {


	static final String VPC_ID_PROPERTY = "vpcId";





	public void scanVPC(String vpcId) {
		try {
			AmazonEC2 ec2 = getClient();
			DescribeVpcsResult vpc = ec2.describeVpcs(new DescribeVpcsRequest().withVpcIds(vpcId));
			vpc.getVpcs().forEach(it -> {
				project(it);
			});

		} catch (AmazonEC2Exception e) {
			if (Strings.nullToEmpty(e.getErrorCode()).equals("InvalidVpcID.NotFound")) {

				getGraphDB().nodes(getEntityTypeName()).id( "account", getAccount(), "vpcId", vpcId).delete();
			} else {
				throw e;
			}
		}

	}

	protected void project(Vpc it) {

		ObjectNode n = toJson(it);

		getGraphDB().nodes(getEntityTypeName()).withTagPrefixes(TAG_PREFIXES).properties( n).idKey("arn").merge();

		getGraphDB().nodes(AwsEntityType.AwsAccount.name()).id("account", getAccount()).relationship("HAS").on("account","account")
				.to(AwsEntityType.AwsVpc.name()).id("arn", n.path("arn").asText()).merge();

		getGraphDB().nodes(AwsEntityType.AwsVpc.name()).id("arn",n.path("arn").asText()).relationship("RESIDES_IN").on("region", "region").to(AwsEntityType.AwsRegion.name()).merge();
	}

	protected Optional<String> toArn(Vpc vpc) {
		return Optional.ofNullable(String.format("arn:aws:ec2:%s:%s:vpc/%s", getRegionName(), getAccount(), vpc.getVpcId()));
	}
	
	private void scanVPCs(AmazonEC2 ec2) {

		ec2.describeVpcs().getVpcs().forEach(vpc -> {

			project(vpc);

		});
	}

	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);
	
	
		scanVPCs(ec2);
		
	
		gc(getEntityTypeName(),ts);
	}

	



	@Override
	public void doScan(JsonNode entity) {
		
		
		if (isEntityType(entity)) {
			scanVPC(entity.path("vpcId").asText());
		}
		else {
			throw new RebarException("do not know how to handle: "+entity);
		}
		
	}



	@Override
	public void doScan(String id) {
		scanVPC(id);
	}
	
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsVpc;
	}

	@Override
	protected ObjectNode toJson(Vpc awsObject) {
	
		ObjectNode n =  super.toJson(awsObject);
		awsObject.getTags().forEach(it->{
			n.put(TAG_PREFIX+it.getKey(),it.getValue());
		});
		n.remove("tags");
		return n;
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
