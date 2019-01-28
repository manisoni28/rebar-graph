package rebar.graph.aws;

import java.util.Optional;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeVpcEndpointsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcEndpointsResult;
import com.amazonaws.services.ec2.model.VpcEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import rebar.graph.core.RelationshipBuilder.Cardinality;
import rebar.util.Json;

public class VpcEndpointScanner extends AbstractNetworkScanner<VpcEndpoint> {

	@Override
	protected ObjectNode toJson(VpcEndpoint awsObject) {

		ObjectNode n = super.toJson(awsObject);
		ArrayNode securityGroups = Json.arrayNode();
		n.set("securityGroupIds", securityGroups);

		n.path("groups").forEach(it -> {
			securityGroups.add(it.path("groupId").asText());
		});
		n.remove("groups");

		ArrayNode dnsNames = Json.arrayNode();
		n.path("dnsEntries").forEach(it -> {
			String dnsName = it.path("dnsName").asText();
			if (!Strings.isNullOrEmpty(dnsName)) {
				dnsNames.add(dnsName);
			}
		});
		n.set("dnsNames", dnsNames);
		n.remove("dnsEntries");

		return n;
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		doScan(null);
		gc(getEntityType(), ts);
		mergeRelationships();

	}

	private void doScan(String id) {
		DescribeVpcEndpointsRequest request = new DescribeVpcEndpointsRequest();
		if (!Strings.isNullOrEmpty(id)) {
			request.setVpcEndpointIds(ImmutableList.of(id));
		}
		do {
			DescribeVpcEndpointsResult result = getClient().describeVpcEndpoints(request);
			request.setNextToken(result.getNextToken());
			result.getVpcEndpoints().forEach(it -> {
				tryExecute(() -> project(it));
			});
		} while (!Strings.isNullOrEmpty(request.getNextToken()));
	}

	private void project(VpcEndpoint endpoint) {
		ObjectNode n = toJson(endpoint);



		awsGraphNodes().idKey("vpcEndpointId").withTagPrefixes(TAG_PREFIXES).properties(n).merge();
	}

	@Override
	public void scan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("vpcEndpointId").asText();
			scan(id);
		}

	}

	@Override
	public void scan(String id) {
		try {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(id));
			doScan(id);
			mergeRelationships();
		} catch (AmazonEC2Exception e) {
		
			if (isNotFoundException(e)) {
				deleteById(id);
				return;
			}
			throw e;
		}
	}

	private void deleteById(String id) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(id));
		awsGraphNodes().id("vpcEndpointId",id).delete();
	}
	private void mergeRelationships() {

		awsRelationships(AwsEntityType.AwsVpc).relationship("HAS").on("vpcId", "vpcId").to(getEntityTypeName()).merge();
		awsRelationships().relationship("USES").on("securityGroupIds", "groupId", Cardinality.MANY)
				.to("AwsSecurityGroup").merge();
		awsRelationships().relationship("RESIDES_IN").on("subnetIds", "subnetId", Cardinality.MANY).to("AwsSubnet")
				.merge();
		awsRelationships().relationship("USES").on("routeTableIds", "routeTableId", Cardinality.MANY)
				.to("AwsRouteTable").merge();
		// Need to implement ENI
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsVpcEndpoint;
	}

}
