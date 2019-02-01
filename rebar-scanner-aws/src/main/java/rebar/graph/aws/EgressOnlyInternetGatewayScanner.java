package rebar.graph.aws;

import java.util.Optional;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeEgressOnlyInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeEgressOnlyInternetGatewaysResult;
import com.amazonaws.services.ec2.model.EgressOnlyInternetGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.graph.core.RelationshipBuilder.Cardinality;
import rebar.util.Json;

public class EgressOnlyInternetGatewayScanner extends AbstractNetworkScanner<EgressOnlyInternetGateway> {




	@Override
	protected ObjectNode toJson(EgressOnlyInternetGateway awsObject) {
	
		ObjectNode n =  super.toJson(awsObject);
		n.set("id",n.path("egressOnlyInternetGatewayId"));
		ArrayNode vpcAttachments = Json.arrayNode();
		n.path("attachments").forEach(it->{
			String vpcId = it.path("vpcId").asText(null);
			if (!Strings.isNullOrEmpty(vpcId)) {
				vpcAttachments.add(vpcId);
			}
		});
		n.remove("attachments");
		n.set("vpcIds",vpcAttachments);
	
		return n;
	}


	@Override
	protected Optional<String> toArn(EgressOnlyInternetGateway awsObject) {
		return Optional.of(String.format("arn:aws:ec2:%s:%s:egress-only-internet-gateway/%s", getRegionName(),getAccount(),awsObject.getEgressOnlyInternetGatewayId()));
		
	}
	
	@Override
	protected void doMergeRelationships() {
		// do not merge account owner
		awsRelationships().relationship("ATTACHED_TO").on("vpcIds","vpcId",Cardinality.MANY).to(AwsEntityType.AwsVpc.name()).merge();
	}
	@Override
	protected void doScan() {
		
		
		long ts = getGraphDB().getTimestamp();
		DescribeEgressOnlyInternetGatewaysRequest request = new DescribeEgressOnlyInternetGatewaysRequest();
		do {
			DescribeEgressOnlyInternetGatewaysResult result = getClient().describeEgressOnlyInternetGateways(request);
			
			for (com.amazonaws.services.ec2.model.EgressOnlyInternetGateway igw: result.getEgressOnlyInternetGateways()) {
				tryExecute(()->project(igw));
			}
		}
		while (!Strings.isNullOrEmpty(request.getNextToken()));
		
		gc("AwsEgressOnlyInternetGateway",ts);
		doMergeRelationships();
	}

	
	@Override
	public void doScan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("egressOnlyInternetGatewayId").asText(null);
			
			deleteById(id);
		}
		
	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		DescribeEgressOnlyInternetGatewaysRequest request = new DescribeEgressOnlyInternetGatewaysRequest().withEgressOnlyInternetGatewayIds(id);
		try {
			DescribeEgressOnlyInternetGatewaysResult result = getClient().describeEgressOnlyInternetGateways(request);
			
			for (com.amazonaws.services.ec2.model.EgressOnlyInternetGateway igw: result.getEgressOnlyInternetGateways()) {
				tryExecute(()->project(igw));
			}
		}
		catch (AmazonEC2Exception e) {
			if (Strings.nullToEmpty(e.getErrorCode()).contains("NotFound")) {
				deleteById(id);
				return;
			}
			throw e;
		}
		
		doMergeRelationships();
	}

	private void deleteById(String id) {
		logger.info("deleting {} id={}",AwsEntityType.AwsEgressOnlyInternetGateway.name(),id);
		awsGraphNodes().id("egressOnlyInternetGatewayId",id).delete();
	}
	protected void project(EgressOnlyInternetGateway igw) {
		ObjectNode n = toJson(igw);
		
		awsGraphNodes().idKey("arn").properties(n).merge();
		
	}


	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsEgressOnlyInternetGateway;
	}





}
