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
		scan((String) WILDCARD);
		gc(getEntityType(), ts);
		doMergeRelationships();

	}

	void doScan(String id) {
		checkScanArgument(id);
		try {
			DescribeVpcEndpointsRequest request = new DescribeVpcEndpointsRequest();

			if (!isWildcard(id)) {
				request.setVpcEndpointIds(ImmutableList.of(id));
			}
			do {
				DescribeVpcEndpointsResult result = getClient().describeVpcEndpoints(request);
				request.setNextToken(result.getNextToken());
				result.getVpcEndpoints().forEach(it -> {
					if (isWildcard(id)) {
						tryExecute(() -> project(it));
					} else {
						project(it);
					}
				});
			} while (!Strings.isNullOrEmpty(request.getNextToken()));
		} catch (AmazonEC2Exception e) {

			if (isNotFoundException(e)) {
				deleteById(id);
				return;
			}
			throw e;
		}
		mergeRelationships();
	}

	protected void project(VpcEndpoint endpoint) {
		ObjectNode n = toJson(endpoint);

		awsGraphNodes().idKey("vpcEndpointId").withTagPrefixes(TAG_PREFIXES).properties(n).merge();
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("vpcEndpointId").asText();
			if (!Strings.isNullOrEmpty(id)) {
				scan(id);
			}
		}

	}

	private void deleteById(String id) {
		checkScanArgument(id);

		awsGraphNodes().id("vpcEndpointId", id).delete();
	}

	@Override
	protected void doMergeRelationships() {

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
