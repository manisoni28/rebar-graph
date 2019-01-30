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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.core.GraphDB;
import rebar.util.Json;

public class RegionScanner extends AwsEntityScanner<Regions> {

	

	@Override
	public void doScan() {
		scanRegions(getClient(AmazonEC2ClientBuilder.class));

	}
	private void scanRegions(AmazonEC2 ec2) {

		ec2.describeRegions().getRegions().forEach(r -> {

			getGraphDB().nodes("AwsRegion").properties(Json.objectNode().put("name", r.getRegionName()).put("region", r.getRegionName()).put(GraphDB.ENTITY_GROUP, "aws").put(GraphDB.ENTITY_TYPE, getEntityTypeName())).idKey("region").merge();

		});

	}
	@Override
	public void doScan(JsonNode entity) {
		// do nothing

	}

	protected Optional<String> toArn(Regions awsEntity) {
		return Optional.empty();
	}

	@Override
	public void doScan(String id) {
		// do nothing
		
	}
	
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsRegion;
	}
	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}

}
