package rebar.graph.aws;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.machinezoo.noexception.Exceptions;

import rebar.graph.core.GraphDB;
import rebar.graph.core.GraphDB.NodeOperation;
import rebar.util.Json;

public class IamUserScanner extends AwsEntityScanner<User> {

	AmazonIdentityManagementClient getClient() {
		return (AmazonIdentityManagementClient) getClient(AmazonIdentityManagementClientBuilder.class);

	}

	private void project(User user) {
		JsonNode n = toJson(user);

		awsGraphNodesWithoutRegion().idKey("arn").properties(n).merge();

	}

	@Override
	protected void doMergeRelationships() {
		mergeAccountOwner();
	}

	@Override
	protected ObjectNode toJson(User awsObject) {

		ObjectNode n = super.toJson(awsObject);
		n.remove("region"); // user is not regional

		return n;
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		ListUsersRequest request = new ListUsersRequest();
		do {
			ListUsersResult result = getClient().listUsers(request);
			result.getUsers().forEach(it -> {
				tryExecute(()->project(it));
			});
			request.setMarker(result.getMarker());
		} while (!Strings.isNullOrEmpty(request.getMarker()));
		gcWithoutRegion(AwsEntityType.AwsIamUser.name(), ts + 1000); // do not match on region

		doMergeRelationships();
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityType(entity)) {

			String name = entity.path("userName").asText();
		
			doScan(name);
		}

	}

	@Override
	public void doScan(String name) {
		checkScanArgument(name);
		try {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "name must be specified");
			GetUserResult result = getClient().getUser(new GetUserRequest().withUserName(name));
			project(result.getUser());
		} catch (NoSuchEntityException e) {
			deleteByName(name);
		}
	}

	private void deleteByName(String name) {
		getGraphDB().nodes(AwsEntityType.AwsIamUser.name()).id("account",getAccount()).id("userName",name).delete();
	}
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsIamUser;
	}

}
