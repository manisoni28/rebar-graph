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

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

public class LaunchConfigScanner extends AbstractEntityScanner<LaunchConfiguration> {

	public LaunchConfigScanner(AwsScanner scanner) {
		super(scanner);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doScan() {
		// TODO Auto-generated method stub

	}

	protected Optional<String> toArn(LaunchConfiguration lc) {
		return Optional.ofNullable(lc.getLaunchConfigurationARN());
	}

	@Override
	public void scan(JsonNode entity) {
		if (isEntityType(entity, AwsEntities.LAUNCH_CONFIG_TYPE)) {
			scanLaunchConfigByName(entity.path("name").asText());
		}

	}

	public void scanLaunchConfigs() {
		long ts = getGraphDB().getTimestamp();
		AmazonAutoScalingClient client = getClient(AmazonAutoScalingClientBuilder.class);

		DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest();

		do {
			DescribeLaunchConfigurationsResult result = client.describeLaunchConfigurations(request);
			result.getLaunchConfigurations().forEach(lc -> {
				project(lc);
			});
			request.setNextToken(result.getNextToken());
		} while (!Strings.isNullOrEmpty(request.getNextToken()));

		gc(AwsEntities.LAUNCH_CONFIG_TYPE, ts);
	}
	
	public void scanLaunchConfigByName(String name) {
		AmazonAutoScalingClient client = getClient(AmazonAutoScalingClientBuilder.class);

		DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest();
		request.withLaunchConfigurationNames(name);
		DescribeLaunchConfigurationsResult result = client.describeLaunchConfigurations(request);
		if (result.getLaunchConfigurations().isEmpty()) {
			getGraphDB().nodes().label(AwsEntities.LAUNCH_CONFIG_TYPE)
			.id("name", name, "region", getRegionName(), "account", getAccount()).delete();
		} else {
			result.getLaunchConfigurations().forEach(lc -> {
				project(lc);
			});
		}

	}

	public void project(LaunchConfiguration lc) {
		ObjectNode n = toJson(lc);

		getGraphDB().nodes(AwsEntities.LAUNCH_CONFIG_TYPE).idKey("arn").properties(n).merge();

	}

}
