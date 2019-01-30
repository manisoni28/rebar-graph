package rebar.graph.aws;

import com.amazonaws.services.apigateway.AmazonApiGatewayClient;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.GetRestApiRequest;
import com.amazonaws.services.apigateway.model.GetRestApiResult;
import com.amazonaws.services.apigateway.model.GetRestApisRequest;
import com.amazonaws.services.apigateway.model.GetRestApisResult;
import com.amazonaws.services.apigateway.model.NotFoundException;
import com.amazonaws.services.apigateway.model.RestApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.util.Json;

public class ApiGatewayRestApiScanner extends AwsEntityScanner<RestApi> {

	AmazonApiGatewayClient getClient() {
		return getClient(AmazonApiGatewayClientBuilder.class);
	}

	@Override
	protected ObjectNode toJson(RestApi x) {
		ObjectNode n = super.toJson(x);
		n.set("endpointConfigurationTypes", n.path("endpointConfiguration").path("types"));
		n.remove("endpointConfiguration");

		return n;
	}

	protected void project(GetRestApiResult api) {
		ObjectNode n = Json.objectMapper().valueToTree(api);
		n.put("graphEntityType", AwsEntityType.AwsApiGatewayRestApi.name());
		n.put("graphEntityGroup", "aws");
		n.put("account", getAccount());
		n.put("region", getRegionName());
		n.set("endpointConfigurationTypes", n.path("endpointConfiguration").path("types"));
		n.remove("endpointConfiguration");
		n.remove("sdkHttpMetadata");
		n.remove("sdkResponseMetadata");
		awsGraphNodes(AwsEntityType.AwsApiGatewayRestApi.name()).idKey("id").properties(n).merge();
		
	}
	protected void project(RestApi d) {
		ObjectNode n = toJson(d);
		
		awsGraphNodes(AwsEntityType.AwsApiGatewayRestApi.name()).idKey("id").properties(n).merge();
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		GetRestApisRequest request = new GetRestApisRequest();
		do {

			GetRestApisResult result = getClient().getRestApis(request);

			result.getItems().forEach(it -> {

				tryExecute(() -> project(it));
				tryExecute(() -> doScan(it.getId()));
			});
			request.setPosition(result.getPosition());
		} while (!Strings.isNullOrEmpty(request.getPosition()));
		gc(AwsEntityType.AwsApiGatewayRestApi.name(), ts);
		mergeAccountOwner(AwsEntityType.AwsApiGatewayRestApi);
	}

	@Override
	public void doScan(JsonNode entity) {

		if (isEntityOwner(entity)) {
			String id = entity.path("id").asText();
			doScan(id);
		}
	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		logger.info("scan id={}", id);
		try {
			GetRestApiRequest request = new GetRestApiRequest().withRestApiId(id);
			GetRestApiResult result = getClient().getRestApi(request);
			project(result);
			mergeAccountOwner(AwsEntityType.AwsApiGatewayRestApi);
		} catch (NotFoundException e) {
			deleteRestApi(id);
		}

	}

	private void deleteRestApi(String id) {
		awsGraphNodes(AwsEntityType.AwsApiGatewayRestApi.name()).id("id", id).delete();
	}

	@Override
	public AwsEntityType getEntityType() {
		// TODO Auto-generated method stub
		return AwsEntityType.AwsApiGatewayRestApi;
	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}

}
