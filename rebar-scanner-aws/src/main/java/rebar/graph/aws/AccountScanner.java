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

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AccountAttribute;
import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.core.GraphDB;
import rebar.util.Json;

public class AccountScanner extends AwsEntityScanner<AccountAttribute,AmazonEC2Client> {



	@Override
	public void doScan() {
	
		getAwsScanner().getRebarGraph().getGraphDB().nodes("AwsAccount").idKey("account").properties(
				Json.objectNode().put("account", getAwsScanner().getAccount()).put(GraphDB.ENTITY_TYPE, "AwsAccount").put(GraphDB.ENTITY_TYPE,"AwsAccount").put(GraphDB.ENTITY_GROUP,"aws")).merge();
	}

	@Override
	public void doScan(JsonNode entity) {
		scan();
	}
	
	protected Optional<String> toArn(AccountAttribute awsEntity) {

		return Optional.empty();
	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		scan();	
	}



	@Override
	protected void doMergeRelationships() {
		
		
	}

	@Override
	protected AmazonEC2Client getClient() {
		return getClient(AmazonEC2ClientBuilder.class);
	}

	@Override
	protected void project(AccountAttribute t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsAccount;
	}
	
	

}
