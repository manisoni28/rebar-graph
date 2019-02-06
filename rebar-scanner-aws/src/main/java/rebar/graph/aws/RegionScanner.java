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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Region;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import rebar.graph.core.GraphDB;
import rebar.util.Json;

public class RegionScanner extends AwsEntityScanner<Region, AmazonEC2Client> {

	@Override
	public void doScan() {
		scanRegions(getClient(AmazonEC2ClientBuilder.class));

	}

	private void scanRegions(AmazonEC2 ec2) {

		ec2.describeRegions().getRegions().forEach(r -> {
			project(r);

		});
		projectAccountRegions();
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

	public AmazonEC2Client getClient() {
		return getClient(AmazonEC2ClientBuilder.class);
	}

	@Override
	protected void project(Region r) {
		getGraphDB().nodes("AwsRegion")
				.properties(Json.objectNode().put("name", r.getRegionName()).put("region", r.getRegionName())
						.put(GraphDB.ENTITY_GROUP, "aws").put(GraphDB.ENTITY_TYPE, getEntityTypeName()))
				.idKey("region").merge();

	}

	protected void projectAccountRegions() {
		// for each account, make sure there are AwsAccountRegion entries for each
		// fastest if we just load the list of regions, and find the non-existing

		Set<String> regions = getGraphDB().getNeo4jDriver().cypher("match (a:AwsRegion) return a.name as name").stream()
				.map(it -> it.path("name").asText()).collect(Collectors.toSet());
		
		Set<String> accounts = getGraphDB().getNeo4jDriver().cypher("match (a:AwsAccount) return a.account as account").stream()
				.map(it->it.path("account").asText()).collect(Collectors.toSet());
		
		List<JsonNode> accountRegions = getGraphDB().getNeo4jDriver().cypher("match (a:AwsAccountRegion) return a").stream().collect(Collectors.toList());
		
		
		for (String account: accounts) {
			Set<String> missingRegions = findMissingRegions(account, regions, accountRegions);
			
			logger.info("missing AwsAccountRegion for account={}: {}",account,missingRegions);
			
			for (String region: missingRegions) {
				String arn = "arn:aws::"+account+":"+region;
				ObjectNode n = Json.objectNode();
				n.put("arn", arn);
				n.put("region", region);
				n.put("account",account);
				n.put("graphEntityType", AwsEntityType.AwsAccountRegion.name());
				n.put("graphEntityGroup","aws");
				
				getGraphDB().nodes(AwsEntityType.AwsAccountRegion.name()).idKey("arn").properties(n).merge();
			}
			
			getGraphDB().nodes(AwsEntityType.AwsAccountRegion.name()).relationship("RESIDES_IN").on("region", "region").to(AwsEntityType.AwsRegion.name()).merge();
			getGraphDB().nodes(AwsEntityType.AwsAccount.name()).relationship("HAS").on("account", "account").to(AwsEntityType.AwsAccountRegion.name()).merge();
			
		}
		
	}
	Set<String> findMissingRegions(String account, Set<String> regions, List<JsonNode> accountRegions) {
		
		List<String> missing = Lists.newArrayList();
		
	
		Set<String> foundRegions = accountRegions.stream().filter(p->p.path("account").asText().equals(account)).map(t->t.path("region").asText()).collect(Collectors.toSet());
			
		return Sets.difference(regions, foundRegions);
		
		
	}

}
