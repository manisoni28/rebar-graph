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

import java.util.Date;
import java.util.List;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.node.ArrayNode;

import rebar.util.Json;

public class AwsEntities {

	String account;
	Regions region;
	
	public static final String REGION_TYPE="AwsRegion";
	public static final String ACCOUNT_TYPE="AwsAccount";
	public static final String LAUNCH_CONFIG_TYPE="AwsLaunchConfig";
	public static final String LAUNCH_TEMPLATE_TYPE="AwsLaunchTemplate";
	public static final String ASG_TYPE="AwsAsg";
	public static final String EC2_INSTANCE_TYPE="AwsEc2Instance";
	public static final String SECURITY_GROUP_TYPE="AwsSecurityGroup";
	public static final String VPC_TYPE="AwsVpc";
	public static final String SUBNET_TYPE="AwsSubnet";
	private AwsEntities(String account, Regions region) {
		this.account = account;
		this.region = region;
	}

	private static AwsEntities forAccount(String account, Regions region) {
		AwsEntities arn = new AwsEntities(account, region);
		return arn;
	}

	public String getAccount() {
		return account;
	}

	public String getRegionName() {
		return region.getName();
	}


	









	

	private long toEpoch(Date d) {
		return d.getTime();
	}
	


	private ArrayNode toArrayNode(List<String> list) {
		ArrayNode n = Json.objectMapper().createArrayNode();
		if (list!=null) {
			list.forEach(it->{
				n.add(it);
			});
		}
		return n;
	}
	

	


}
