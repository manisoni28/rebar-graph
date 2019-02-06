package rebar.graph.aws;

import java.util.List;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.machinezoo.noexception.Exceptions;

import rebar.util.Json;

public class RdsClusterScanner extends AwsEntityScanner<DBCluster, AmazonRDSClient> {

	static void fixBrokenNames(ObjectNode n) {
		List<String> brokenNames = Lists.newArrayList();
		n.fieldNames().forEachRemaining(it -> {
			if (it.startsWith("db") && it.length() >= 3) {

				if (it.equals("dbiResourceId")) {
					// do nothing
				} else {
					brokenNames.add(it);
				}
			}
		});

		brokenNames.forEach(it -> {
			JsonNode val = n.get(it);

			String fixedName = "db" + (it.substring(2, 3).toUpperCase()) + it.substring(3);
			n.remove(it);
			n.set(fixedName, val);
		});

		if (n.has("iamdatabaseAuthenticationEnabled")) {
			n.set("iamDatabaseAuthenticationEnabled", n.get("iamDatabaseAuthenticationEnabled"));
			n.remove("iamdatabaseAuthenticationEnabled");
		}

		if (n.has("cacertificateIdentifier")) {
			n.set("caCertificateIdentifier", n.get("cacertificateIdentifier"));
			n.remove("cacertificateIdentifier");
		}
	}

	@Override
	protected ObjectNode toJson(DBCluster awsObject) {

		ObjectNode n = super.toJson(awsObject);

		fixBrokenNames(n);
		n.put("arn", awsObject.getDBClusterArn());

		return n;
	}

	protected AmazonRDSClient getClient() {

		return getClient(AmazonRDSClientBuilder.class);
	}

	@Override
	protected void doScan() {
		long ts = getGraphBuilder().getTimestamp();
		AmazonRDSClient rds = getClient(AmazonRDSClientBuilder.class);
		DescribeDBClustersRequest request = new DescribeDBClustersRequest();

		do {
			DescribeDBClustersResult result = rds.describeDBClusters(request);
			result.getDBClusters().forEach(it -> {
				tryExecute(() -> project(it));
			});
			request.setMarker(result.getMarker());
		} while (!Strings.isNullOrEmpty(request.getMarker()));
		gc("AwsRdsCluster", ts);

		mergeAccountOwner();

	}

	protected void project(DBCluster cluster) {
		ObjectNode n = toJson(cluster);

		getGraphBuilder().nodes("AwsRdsCluster").id("account", getAccount()).id("region", getRegionName())
				.id("dbClusterIdentifier", cluster.getDBClusterIdentifier()).properties(n).merge();

	}

	@Override
	public void doScan(JsonNode entity) {
		doScan(entity.path("dbClusterIdentifier").asText());
	}

	@Override
	public void doScan(String dbClusterIdentifier) {
		checkScanArgument(dbClusterIdentifier);
		try {
			AmazonRDSClient rds = getClient(AmazonRDSClientBuilder.class);
			DescribeDBClustersRequest request = new DescribeDBClustersRequest();
			request.withDBClusterIdentifier(dbClusterIdentifier);
			DescribeDBClustersResult result = rds.describeDBClusters(request);
			result.getDBClusters().forEach(it -> {
				project(it);

			});
			request.setMarker(result.getMarker());
		} catch (DBClusterNotFoundException e) {
			getGraphBuilder().nodes("AwsRdsCluster").id("account", getAccount()).id("region", getRegionName())
					.id("dbclusterIdentifier", dbClusterIdentifier).delete();
		}
		mergeAccountOwner();
	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub

	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsRdsCluster;
	}

}
