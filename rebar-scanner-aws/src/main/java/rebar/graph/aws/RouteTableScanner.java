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
		doScan(WILDCARD);
		doMergeRelationships();
	}

	protected void doScan(String id) {
		checkScanArgument(id);
		try {
		long ts = getGraphDB().getTimestamp();
		DescribeRouteTablesRequest req = new DescribeRouteTablesRequest();
		
		if (isWildcard(id)) {
			// set nothing
		}
		else {
			req.withRouteTableIds(id);
		}
		do {
			DescribeRouteTablesResult result = getClient().describeRouteTables(req);
			result.getRouteTables().forEach(it -> {
				tryExecute(() -> project(it));
			});
			req.setNextToken(result.getNextToken());
		} while (!Strings.isNullOrEmpty(req.getNextToken()));

		if (Strings.isNullOrEmpty(id)) {
			gc(AwsEntityType.AwsRouteTable,ts);
		}
		}
		catch(AmazonEC2Exception e) {
			if (isNotFoundException(e)) {
				deleteById(id);
				return;
			}
			throw e;
			
		}
	}

	@Override
	protected void doMergeRelationships() {

		awsRelationships(AwsEntityType.AwsVpc.name()).relationship("HAS").on("vpcId", "vpcId")
				.to(getEntityType().name()).merge();

		
		// our relationship builder can't reverse the relationship (yet) so we do it manually
		getGraphDB().getNeo4jDriver().cypher("match (s:AwsSubnet {account:{account},region:{region}}),(r:AwsRouteTable {account:{account},region:{region}})"
				+ " where s.subnetId in r.associatedSubnets merge (s)-[x:USES]->(r)")
		.param("region",getRegionName()).params("account",getAccount()).exec();
		
		getGraphDB().getNeo4jDriver().cypher("match (s:AwsSubnet {account:{account},region:{region}})-[x:USES]->(r) where NOT s.subnetId in r.associatedSubnets delete x")
		.param("region",getRegionName()).params("account",getAccount()).exec();
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("routeTableId").asText();
			doScan(id);
		}

	}

	protected void project(RouteTable routeTable) {
		ObjectNode n = toJson(routeTable);

		awsGraphNodes(AwsEntityType.AwsRouteTable).idKey("arn").withTagPrefixes(TAG_PREFIXES).properties(n).merge();

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
