package rebar.graph.aws;

import java.util.ArrayList;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeVpcPeeringConnectionsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcPeeringConnectionsResult;
import com.amazonaws.services.ec2.model.VpcPeeringConnection;
import com.amazonaws.services.ec2.model.VpcPeeringConnectionVpcInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import rebar.util.Json;

public class VpcPeeringConnectionScanner extends AbstractNetworkScanner<VpcPeeringConnection> {

	@Override
	protected ObjectNode toJson(VpcPeeringConnection awsObject) {

		ObjectNode n = super.toJson(awsObject);
		n.put("expirationTs", awsObject.getExpirationTime().getTime());

		n.set("statusCode", n.path("status").path("code"));
		n.set("statusMessage", n.path("status").path("message"));
		n.remove("status");

		awsObject.getTags().forEach(it -> {
			n.put("tag_" + it.getKey(), it.getValue());
		});
		n.remove("tags");

		{
			VpcPeeringConnectionVpcInfo requester = awsObject.getRequesterVpcInfo();

			{
				ArrayNode requesterCidrBlockSet = Json.arrayNode();
				requester.getCidrBlockSet().forEach(it -> {
					requesterCidrBlockSet.add(it.getCidrBlock());
				});
				n.set("requesterCidrBlockSet", requesterCidrBlockSet);
			}

			{
				ArrayNode requesterIpv6CidrBlockSet = Json.arrayNode();
				requester.getIpv6CidrBlockSet().forEach(it -> {
					requesterIpv6CidrBlockSet.add(it.getIpv6CidrBlock());
				});
				n.set("requesterIpv6CidrBlockSet", requesterIpv6CidrBlockSet);
			}

			n.set("requesterVpcId", n.path("requesterVpcInfo").path("vpcId"));
			n.set("requesterRegion", n.path("requesterVpcInfo").path("region"));
			n.set("requesterOwnerId", n.path("requesterVpcInfo").path("ownerId"));
		}
		{
			VpcPeeringConnectionVpcInfo accepter = awsObject.getAccepterVpcInfo();
			{
				ArrayNode cidrBlockSet = Json.arrayNode();
				accepter.getCidrBlockSet().forEach(it -> {
					cidrBlockSet.add(it.getCidrBlock());
				});
				n.set("accepterCidrBlockSet", cidrBlockSet);
			}
			{
				ArrayNode ipv6CidrBlockSet = Json.arrayNode();
				accepter.getIpv6CidrBlockSet().forEach(it -> {
					ipv6CidrBlockSet.add(it.getIpv6CidrBlock());
				});
				n.set("accepterIpv6CidrBlockSet", ipv6CidrBlockSet);
			}

			n.set("accepterVpcId", n.path("requesterVpcInfo").path("vpcId"));
			n.set("accepterRegion", n.path("requesterVpcInfo").path("region"));
			n.set("accepterOwnerId", n.path("requesterVpcInfo").path("ownerId"));
		}
		return n;
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		scan((String) WILDCARD);

		gc(AwsEntityType.AwsVpcPeeringConnection, ts);
	}

	void doScan(String id) {
		try {

			DescribeVpcPeeringConnectionsRequest request = new DescribeVpcPeeringConnectionsRequest();
			if (isWildcard(id)) {
				// do nothing
			}
			else {
				request.withVpcPeeringConnectionIds(id);
			}
			DescribeVpcPeeringConnectionsResult result = getClient().describeVpcPeeringConnections(request);
			result.getVpcPeeringConnections().forEach(it -> {
				tryExecute(() -> project(it));
			});
		} catch (AmazonEC2Exception e) {
			if (isNotFoundException(e)) {
				deleteById(id);
				return;
			}
			throw e;
		}
	}

	void project(VpcPeeringConnection c) {

		ObjectNode n = toJson(c);
		awsGraphNodes().idKey("vpcPeeringConnectionId").properties(n).merge();
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("vpcPeeringConnectionId").asText();
			scan(id);
		}

	}


	private void deleteById(String id) {
		checkScanArgument(id);

		awsGraphNodes().id("vpcPeeringConnectionId", id).delete();
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsVpcPeeringConnection;
	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}

}
