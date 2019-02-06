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

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

public class LaunchConfigScanner extends AwsEntityScanner<LaunchConfiguration,AmazonAutoScalingClient> {

	



	protected Optional<String> toArn(LaunchConfiguration lc) {
		return Optional.ofNullable(lc.getLaunchConfigurationARN());
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityType(entity, AwsEntityType.AwsLaunchConfig.name())) {
			scanLaunchConfigByName(entity.path("name").asText());
		}

	}

	public AmazonAutoScalingClient getClient() {
		return getClient(AmazonAutoScalingClientBuilder.class);
	}
	public void doScan() {
		long ts = getGraphBuilder().getTimestamp();
		AmazonAutoScalingClient client = getClient();

		DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest();

		do {
			DescribeLaunchConfigurationsResult result = client.describeLaunchConfigurations(request);
			result.getLaunchConfigurations().forEach(lc -> {
				tryExecute(()->project(lc));
			});
			request.setNextToken(result.getNextToken());
		} while (!Strings.isNullOrEmpty(request.getNextToken()));

		gc(getEntityTypeName(), ts);
		mergeRelationships();
	}
	
	public void scanLaunchConfigByName(String name) {
		checkScanArgument(name);
		AmazonAutoScalingClient client = getClient(AmazonAutoScalingClientBuilder.class);

		DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest();
		request.withLaunchConfigurationNames(name);
		DescribeLaunchConfigurationsResult result = client.describeLaunchConfigurations(request);
		if (result.getLaunchConfigurations().isEmpty()) {
			getGraphBuilder().nodes(getEntityTypeName())
			.id("name", name, "region", getRegionName(), "account", getAccount()).delete();
		} else {
			result.getLaunchConfigurations().forEach(lc -> {
				project(lc);
			});
		}
		mergeRelationships();
	}

	public void project(LaunchConfiguration lc) {
		ObjectNode n = toJson(lc);

		getGraphBuilder().nodes(getEntityTypeName()).idKey("arn").properties(n).merge();

	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		scanLaunchConfigByName(id);
		mergeRelationships();
	}
	
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsLaunchConfig;
	}

	@Override
	protected void doMergeRelationships() {
		
		// AsgScanner will take care of the other relationships.
		mergeAccountOwner(getEntityType());
	}

	@Override
	protected ObjectNode toJson(LaunchConfiguration awsObject) {
		// TODO Auto-generated method stub
		ObjectNode n =  super.toJson(awsObject);
		
		if (n.has("launchConfigurationARN") && !n.has("launchConfigurationArn")) {
			n.set("launchConfigurationArn", n.path("launchConfigurationARN"));
		}
		return n;
	}

}
