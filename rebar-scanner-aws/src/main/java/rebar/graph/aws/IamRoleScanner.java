package rebar.graph.aws;

import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.machinezoo.noexception.Exceptions;

import rebar.graph.core.GraphBuilder;
import rebar.graph.core.GraphBuilder.NodeOperation;
import rebar.util.Json;

public class IamRoleScanner extends AwsEntityScanner<Role,AmazonIdentityManagementClient> {

	protected AmazonIdentityManagementClient getClient() {
		return (AmazonIdentityManagementClient) getClient(AmazonIdentityManagementClientBuilder.class);

	}

	protected static String urlDecode(String s) {
		try {
			s= URLDecoder.decode(s, "UTF-8");
			return s;
		} catch (Exception e) {
			return s;
		}
	}
	protected void project(Role role) {
		ObjectNode n = toJson(role);

		
		awsGraphNodesWithoutRegion().idKey("arn").properties(n).merge();

	}

	@Override
	protected void doMergeRelationships() {
		mergeAccountOwner();
	}

	@Override
	protected ObjectNode toJson(Role awsObject) {

		ObjectNode n = super.toJson(awsObject);
		n.remove("region"); // user is not regional
		n.put("assumeRolePolicyDocument",urlDecode(n.path("assumeRolePolicyDocument").asText()));
	
		return n;
	}

	@Override
	protected void doScan() {

		long ts = getGraphBuilder().getTimestamp();
		ListRolesRequest request = new ListRolesRequest();
		do {
			ListRolesResult result = getClient().listRoles(request);
			result.getRoles().forEach(it -> {
				tryExecute(()->project(it));
			});
			if (result.isTruncated()) {
				result.setMarker(request.getMarker());
			}
			request.setMarker(result.getMarker());
		} while (!Strings.isNullOrEmpty(request.getMarker()));
		gcWithoutRegion(AwsEntityType.AwsIamRole.name(),ts); // do not match on region

		doMergeRelationships();
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityType(entity)) {

			String name = entity.path("roleName").asText();
		
			doScan(name);
		
		}

	}

	@Override
	public void doScan(String name) {
		try {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "name must be specified");
			GetRoleResult result = getClient().getRole(new GetRoleRequest().withRoleName(name));
			project(result.getRole());
		} catch (NoSuchEntityException e) {
			deleteByName(name);
		}
		doMergeRelationships();
	}

	private void deleteByName(String name) {
		getGraphBuilder().nodes(getEntityType().name()).id("account",getAccount()).id("roleName",name).delete();
	}
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsIamRole;
	}

}
