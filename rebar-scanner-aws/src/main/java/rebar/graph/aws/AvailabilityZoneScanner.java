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
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.core.GraphBuilder;
import rebar.util.Json;

public class AvailabilityZoneScanner extends AwsEntityScanner<AvailabilityZone,AmazonEC2Client> {



	@Override
	public void doScan() {
		scanAvailabilityZones(getClient(AmazonEC2ClientBuilder.class));

	}

	@Override
	public void doScan(JsonNode entity) {
		scan();
	}

	protected Optional<String> toArn(AvailabilityZone awsEntity) {
		return Optional.empty();
	}

	private void scanAvailabilityZones(AmazonEC2 ec2) {
		ec2.describeAvailabilityZones().getAvailabilityZones().forEach(it -> {

			getGraphBuilder()
					.nodes("AwsAvailabilityZone").properties(Json.objectNode().put("region", it.getRegionName())
							.put("name", it.getZoneName()).put(GraphBuilder.ENTITY_TYPE, "AwsAvailabilityZone").put(GraphBuilder.ENTITY_GROUP, "aws"))
					.idKey("name").merge();

		});

		getGraphBuilder().nodes("AwsRegion").relationship("HAS").on("region", "region").to("AwsAvailabilityZone").merge();
	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		
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
	protected void project(AvailabilityZone t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsAvailabilityZone;
	}


}
