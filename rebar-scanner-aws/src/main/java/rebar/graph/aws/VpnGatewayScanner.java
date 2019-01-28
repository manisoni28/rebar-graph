package rebar.graph.aws;

import java.util.ArrayList;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeVpnGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeVpnGatewaysResult;
import com.amazonaws.services.ec2.model.VpnGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import rebar.graph.core.RelationshipBuilder.Cardinality;
import rebar.util.Json;

public class VpnGatewayScanner extends AbstractNetworkScanner<VpnGateway> {



	@Override
	protected void doScan() {
		long ts = getGraphDB().getTimestamp();
		doScan(null);
		mergeRelationships();
		gc(getEntityType(),ts);
	}

	private void doScan(String id) {
		DescribeVpnGatewaysRequest request = new DescribeVpnGatewaysRequest();
		if (!Strings.isNullOrEmpty(id)) {
			request.setVpnGatewayIds(ImmutableList.of(id));
		}
		DescribeVpnGatewaysResult result = getClient().describeVpnGateways(request);
		result.getVpnGateways().forEach(it->{
			tryExecute(()->project(it));
		});
	}
	@Override
	public void scan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("vpnGatewayId").asText();
			scan(id);
		}
		
	}

	@Override
	public void scan(String id) {
		try {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(id));
		doScan(id);
		}
		catch (AmazonEC2Exception e) {
			if (isNotFoundException(e)) {
				deleteById(id);
				return;
			}
			throw e;
		}
		mergeRelationships();
	}

	private void deleteById(String id) {
		logger.info("deleting {} id={}",getEntityType().name(),id);
		awsGraphNodes().id("vpnGatewayId",id).delete();
	}
	@Override
	public AwsEntityType getEntityType() {
	
		return AwsEntityType.AwsVpnGateway;
	}

	private void mergeRelationships() {
		
		awsRelationships().relationship("ATTACHED_TO").on("vpcAttachments","vpcId",Cardinality.MANY).to(AwsEntityType.AwsVpc.name()).merge();
	}
	
	private void project(VpnGateway gw) {
		ObjectNode n = toJson(gw);
		
		
		awsGraphNodes().idKey("vpnGatewayId").withTagPrefixes(TAG_PREFIXES).properties(n).merge();
	}
	
	@Override
	protected ObjectNode toJson(VpnGateway awsObject) {
		
		ObjectNode n =  super.toJson(awsObject);
		awsObject.getTags().forEach(it->{
			n.put(TAG_PREFIX+it.getKey(), it.getValue());
		});
		n.remove("tags");
		
		ArrayNode vpcAttachments = Json.arrayNode();
		awsObject.getVpcAttachments().forEach(it->{
			vpcAttachments.add(it.getVpcId());
		});
		n.remove("vpcAttachments");
		n.set("vpcAttachments", vpcAttachments);
		return n;
	}
}
