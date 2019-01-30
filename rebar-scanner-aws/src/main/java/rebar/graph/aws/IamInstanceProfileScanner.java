package rebar.graph.aws;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileResult;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.machinezoo.noexception.Exceptions;

import rebar.graph.core.GraphDB;
import rebar.graph.core.GraphDB.NodeOperation;
import rebar.graph.core.RelationshipBuilder.Cardinality;
import rebar.util.Json;

public class IamInstanceProfileScanner extends AwsEntityScanner<InstanceProfile> {

	AmazonIdentityManagementClient getClient() {
		return (AmazonIdentityManagementClient) getClient(AmazonIdentityManagementClientBuilder.class);

	}

	private void project(InstanceProfile profile) {
		JsonNode n = toJson(profile);

		awsGraphNodesWithoutRegion().idKey("arn").properties(n).merge();

	}

	@Override
	protected void doMergeRelationships() {
		mergeAccountOwner();

		getGraphDB().nodes(getEntityTypeName()).id("account", getAccount()).relationship("USES")
				.on("roleArns", "arn", Cardinality.MANY).to(AwsEntityType.AwsIamRole.name()).id("account", getAccount())
				.merge();

	}

	@Override
	protected ObjectNode toJson(InstanceProfile awsObject) {

		ObjectNode n = super.toJson(awsObject);
		n.remove("region"); // user is not regional

		ArrayNode roleArns = Json.arrayNode();
		n.set("roleArns", roleArns);
		n.path("roles").forEach(it -> {
			if (it.isObject()) {
				ObjectNode role = (ObjectNode) it;
				String assumeRolePolicyDocument = IamRoleScanner
						.urlDecode(it.path("assumeRolePolicyDocument").asText());
				role.put("assumeRolePolicyDocument", assumeRolePolicyDocument);
				String roleArn = role.path("arn").asText();
				if (!Strings.isNullOrEmpty(roleArn)) {
					roleArns.add(roleArn);
				}
			}
		});

		return n;
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		ListInstanceProfilesRequest request = new ListInstanceProfilesRequest();
		do {
			ListInstanceProfilesResult result = getClient().listInstanceProfiles(request);
			result.getInstanceProfiles().forEach(it -> {
				tryExecute(() -> project(it));
			});
			request.setMarker(result.getMarker());
			if (result.isTruncated()) {
				request.setMarker(null);
			}
		} while (!Strings.isNullOrEmpty(request.getMarker()));
		gcWithoutRegion(AwsEntityType.AwsIamInstanceProfile.name(), ts); // do not match on region

		doMergeRelationships();
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityType(entity)) {

			String name = entity.path("instanceProfileName").asText();

			doScan(name);
		}

	}

	@Override
	public void doScan(String name) {
		checkScanArgument(name);
		try {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "instanceProfileName must be specified");
			GetInstanceProfileResult result = getClient()
					.getInstanceProfile(new GetInstanceProfileRequest().withInstanceProfileName(name));
			project(result.getInstanceProfile());
		} catch (NoSuchEntityException e) {
			deleteByName(name);
		}
	}

	private void deleteByName(String name) {
		getGraphDB().nodes(AwsEntityType.AwsIamInstanceProfile.name()).id("account", getAccount())
				.id("instanceProfileName", name).delete();
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsIamInstanceProfile;
	}

}
