package rebar.graph.aws;

import java.util.Optional;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.graph.core.RelationshipBuilder.Cardinality;
import rebar.util.Json;

public class InternetGatewayScanner extends AbstractNetworkScanner<InternetGateway> {

	@Override
	protected void doScan() {

		long ts = getGraphBuilder().getTimestamp();
		for (InternetGateway igw : getClient().describeInternetGateways().getInternetGateways()) {

			tryExecute(() -> project(igw));

		}
		gc(AwsEntityType.AwsInternetGateway.name(), ts);
		doMergeRelationships();
	}

	protected void project(InternetGateway igw) {
		ObjectNode n = toJson(igw);

		awsGraphNodes(AwsEntityType.AwsInternetGateway.name()).idKey("arn").withTagPrefixes(TAG_PREFIXES).properties(n)
				.merge();
	}

	protected void doMergeRelationships() {

		awsRelationships().relationship("ATTACHED_TO").on("vpcIds", "vpcId", Cardinality.MANY).to("AwsVpc").merge();
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("internetGatewayId").asText();
			scan(id);
			doMergeRelationships();
		}

	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		try {
			getClient().describeInternetGateways(new DescribeInternetGatewaysRequest().withInternetGatewayIds(id))
					.getInternetGateways().forEach(it -> {
						project(it);
					});
		} catch (AmazonEC2Exception e) {
			if (Strings.nullToEmpty(e.getErrorCode()).contains("NotFound")) {
				deleteId(id);
				return;
			}
			throw e;
		}

	}

	private void deleteId(String id) {
		logger.info("deleting AwsInternetGateway {}", id);
		getGraphBuilder().getNeo4jDriver().cypher(
				"match (a:AwsInternetGateway {internetGatewayId:{internetGatewayId},account:{account},region:{region}}) detach delete a")
				.param("account", getAccount()).param("region", getRegionName()).param("internetGatewayId", id).exec();
	}

	@Override
	protected Optional<String> toArn(InternetGateway awsObject) {
		return Optional.of(String.format("arn:aws:ec2:%s:%s:internet-gateway/%s", getRegionName(), getAccount(),
				awsObject.getInternetGatewayId()));
	}

	@Override
	protected ObjectNode toJson(InternetGateway awsObject) {

		ObjectNode n = super.toJson(awsObject);
		n.set("id", n.path("internetGatewayId"));
		awsObject.getTags().forEach(it -> {
			n.put(TAG_PREFIX + it.getKey(), it.getValue());
		});
		ArrayNode vpcs = Json.arrayNode();
		n.set("vpcIds", vpcs);
		n.path("attachments").forEach(it -> {
			String vpcId = it.path("vpcId").asText(null);
			if (!Strings.isNullOrEmpty(vpcId)) {
				vpcs.add(vpcId);
			}
		});
		n.remove("attachments");
		n.remove("tags");

		return n;
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsInternetGateway;
	}

}
