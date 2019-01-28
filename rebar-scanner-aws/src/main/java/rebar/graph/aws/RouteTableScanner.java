package rebar.graph.aws;

import java.util.Optional;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.RouteTable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import rebar.graph.core.RelationshipBuilder.Cardinality;
import rebar.util.Json;

public class RouteTableScanner extends AbstractNetworkScanner<RouteTable> {

	@Override
	protected ObjectNode toJson(RouteTable awsObject) {

		ObjectNode n = super.toJson(awsObject);
		n.put("id", awsObject.getRouteTableId());
		{
			n.path("tags").forEach(it -> {
				n.put(TAG_PREFIX + it.path("key").asText(), it.path("value").asText());
			});
			n.remove("tags");
		}

		{
			ArrayNode associatedSubnets = Json.arrayNode();
			n.set("associatedSubnets", associatedSubnets);
			n.path("associations").forEach(it -> {
				String routeTableId = it.path("routeTableId").asText();
				String subnetId = it.path("subnetId").asText();

				if (awsObject.getRouteTableId().equals(routeTableId) && !Strings.isNullOrEmpty(subnetId)) {
					associatedSubnets.add(subnetId);
				}
			});

			n.remove("associations");
		}

		return n;
	}

	@Override
	protected Optional<String> toArn(RouteTable awsObject) {
		return Optional.ofNullable(String.format("arn:aws:ec2:%s:%s:route-table/%s", getRegionName(), getAccount(),
				awsObject.getRouteTableId()));
	}

	@Override
	protected void doScan() {
		doScan(null);
		mergeRelationships();
	}

	private void doScan(String id) {

		long ts = getGraphDB().getTimestamp();
		DescribeRouteTablesRequest req = new DescribeRouteTablesRequest();
		if (!Strings.isNullOrEmpty(id)) {
			req.withRouteTableIds(id);
		}
		do {
			DescribeRouteTablesResult result = getClient().describeRouteTables(req);
			result.getRouteTables().forEach(it -> {
				tryExecute(() -> project(it));
			});
		} while (!Strings.isNullOrEmpty(req.getNextToken()));

		if (Strings.isNullOrEmpty(id)) {
			gc(AwsEntityType.AwsRouteTable,ts);
		}
	}

	private void mergeRelationships() {

		awsRelationships(AwsEntityType.AwsVpc.name()).relationship("HAS").on("vpcId", "vpcId")
				.to(getEntityType().name()).merge();

		awsRelationships().relationship("ATTACHED_TO").on("associatedSubnets", "subnetId", Cardinality.MANY)
				.to("AwsSubnet").merge();
	}

	@Override
	public void scan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("routeTableId").asText();
			scan(id);
		}

	}

	private void project(RouteTable routeTable) {
		ObjectNode n = toJson(routeTable);

		awsGraphNodes(AwsEntityType.AwsRouteTable).idKey("arn").withTagPrefixes(TAG_PREFIXES).properties(n).merge();

	}

	@Override
	public void scan(String id) {
		try {
		doScan(id);
		mergeRelationships();
		
		}
		catch(AmazonEC2Exception e) {
			if (isNotFoundException(e)) {
				deleteById(id);
				return;
			}
			throw e;
			
		}
	}

	private void deleteById(String id) {
		logger.info("deleting route table: {}",id);
		awsGraphNodes().id("routeTableId",id).delete();
	}
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsRouteTable;
	}

}
