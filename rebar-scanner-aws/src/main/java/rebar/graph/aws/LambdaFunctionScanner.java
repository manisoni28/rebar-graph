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

import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationResult;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.VpcConfigResponse;
import com.amazonaws.services.s3.model.GetBucketNotificationConfigurationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.util.Json;

public class LambdaFunctionScanner extends AwsEntityScanner<FunctionConfiguration, AWSLambdaClient> {

	protected void project(FunctionConfiguration f) {
		ObjectNode n = toJson(f);
		getGraphDB().nodes(getEntityTypeName()).id("arn", n.path("arn").asText()).properties(n).merge();
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		AWSLambda c = getClient(AWSLambdaClientBuilder.class);

		ListFunctionsRequest lfRequest = new ListFunctionsRequest();
		do {
			ListFunctionsResult result = c.listFunctions(lfRequest);
			result.getFunctions().forEach(f -> {

				project(f);

			});
			lfRequest.setMarker(result.getNextMarker());
		} while (!Strings.isNullOrEmpty(lfRequest.getMarker()));

		gc("AwsLambdaFunction", ts);

		doMergeRelationships();

		getGraphDB().nodes("AwsLambdaFunction").id("region", getRegionName()).id("account", getAccount())
				.relationship("RESIDES_IN").on("vpcId", "vpcId").to("AwsVpc").id("region", getRegionName()).merge();
	}

	@Override
	protected void doMergeRelationships() {
		mergeResidesInRegionRelationship();
		mergeAccountOwner();

		getGraphDB().nodes("AwsLambdaFunction").id("region", getRegionName()).id("account", getAccount())
				.relationship("RESIDES_IN").on("vpcId", "vpcId").to("AwsVpc").id("region", getRegionName()).merge();

	}

	public void scanByName(String name) {

		AWSLambda c = getClient(AWSLambdaClientBuilder.class);

		GetFunctionConfigurationRequest lfRequest = new GetFunctionConfigurationRequest();
		lfRequest.withFunctionName(name);

		try {

			GetFunctionConfigurationResult result = c.getFunctionConfiguration(lfRequest);

			FunctionConfiguration fc = new FunctionConfiguration();

			fc.setCodeSize(result.getCodeSize());
			fc.setCodeSha256(result.getCodeSha256());
			fc.setDescription(result.getDescription());
			fc.setFunctionArn(result.getFunctionArn());
			fc.setFunctionName(result.getFunctionName());
			fc.setHandler(result.getHandler());
			fc.setKMSKeyArn(result.getKMSKeyArn());
			fc.setLastModified(result.getLastModified());
			fc.setMasterArn(result.getMasterArn());
			fc.setMemorySize(result.getMemorySize());
			fc.setRevisionId(result.getRevisionId());
			fc.setRole(result.getRole());
			fc.setRuntime(result.getRuntime());
			fc.setTimeout(result.getTimeout());
			fc.setVersion(result.getVersion());
			VpcConfigResponse cfg = result.getVpcConfig();
			fc.setEnvironment(result.getEnvironment());

			fc.setTracingConfig(result.getTracingConfig());
			fc.setDeadLetterConfig(result.getDeadLetterConfig());
			fc.setVpcConfig(cfg);

			project(fc);

		} catch (ResourceNotFoundException e) {
			getGraphDB().nodes("AwsLambdaFunction").id("account", getAccount()).id("region", getRegionName())
					.id("name", name).delete();
		}
		doMergeRelationships();
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityType(entity)) {
			scanByName(entity.path("functionName").asText());
		}

	}

	protected ObjectNode toJson(FunctionConfiguration awsObject) {

		ObjectNode n = super.toJson(awsObject);

		n.set("vpcSubnetIds", n.path("vpcConfig").path("subnetIds"));
		n.set("vpcSecurityGroupIds", n.path("vpcConfig").path("sercurityGroupIds"));
		n.put("vpcId", n.path("vpcConfig").path("vpcId").asText(null));
		n.set("name", n.path("functionName"));
		n.set("tracingConfigMode", n.path("tracingConfig").path("mode"));

		n.remove("tracingConfig");
		n.remove("vpcConfig");
		return n;
	}

	@Override
	protected Optional<String> toArn(FunctionConfiguration awsObject) {
		return Optional.ofNullable(awsObject.getFunctionArn());
	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		scanByName(id);

	}

	@Override
	protected AWSLambdaClient getClient() {
		return getClient(AWSLambdaClientBuilder.class);
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsLambdaFunction;
	}

}
