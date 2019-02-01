package rebar.graph.aws;

import java.util.Optional;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.machinezoo.noexception.Exceptions;

import rebar.util.Json;

public class RdsInstanceScanner extends AwsEntityScanner<DBInstance,AmazonRDSClient> {

	

	@Override
	protected Optional<String> toArn(DBInstance awsObject) {
		return Optional.ofNullable(awsObject.getDBInstanceArn());
	}

	@Override
	protected ObjectNode toJson(DBInstance awsObject) {
		ObjectNode n = super.toJson(awsObject);
		RdsClusterScanner.fixBrokenNames(n);
		n.set("endpointAddress", n.path("endpoint").path("address"));
		n.set("endpointPort", n.path("endpoint").path("port"));
		n.set("endpointHostedZoneId", n.path("endpoint").path("hostedZoneId"));
		n.set("status", n.path("dbInstanceStatus"));
		n.set("vpcId",n.path("dbsubnetGroup").path("vpcId"));
		
		ArrayNode an = Json.arrayNode();
		n.path("dbSubnetGroup").path("subnets").forEach(it->{
			String subnetId = it.path("subnetIdentifier").asText();
			if (!Strings.isNullOrEmpty(subnetId)) {
				an.add(subnetId);
			}
		});
		n.set("subnetIds",an);
		return n;
	}

	@Override
	protected void doScan() {
		long ts = System.currentTimeMillis();
		AmazonRDSClient rds = getClient(AmazonRDSClientBuilder.class);
		DescribeDBInstancesRequest request = new DescribeDBInstancesRequest();

		do {

			DescribeDBInstancesResult result = rds.describeDBInstances(request);
			result.getDBInstances().forEach(it -> {
				tryExecute(() -> project(it));
			
			});
			request.setMarker(result.getMarker());
		} while (!Strings.isNullOrEmpty(request.getMarker()));

		gc("AwsRdsInstance", ts);
		mergeClusterInstanceRelationships();
		mergeAccountOwner();
	}

	protected void project(DBInstance instance) {
		
		ObjectNode n = toJson(instance);
	
		getGraphDB().nodes("AwsRdsInstance").id("arn",instance.getDBInstanceArn()).properties(n).merge();
	}

	@Override
	public void doScan(JsonNode entity) {
		
		doScan(entity.path("dbInstanceIdentifier").asText());
	}

	@Override
	public void doScan(String id) {
		doScan(id);
		try {
			AmazonRDSClient rds = getClient(AmazonRDSClientBuilder.class);
			DescribeDBInstancesRequest request = new DescribeDBInstancesRequest();
			request.withDBInstanceIdentifier(id);

			DescribeDBInstancesResult result = rds.describeDBInstances(request);
			result.getDBInstances().forEach(it -> {
				project(it);			
			});
			mergeClusterInstanceRelationships();
			mergeAccountOwner();
		} catch (DBInstanceNotFoundException e) {
			getGraphDB().nodes("AwsRdsInstance").id("account", getAccount()).id("region", getRegionName())
					.id("dbInstanceIdentifier", id).delete();
		}

	}
	
	protected void mergeClusterInstanceRelationships() {
		awsGraphNodes(AwsEntityType.AwsRdsCluster.name()).relationship("HAS").on("dbClusterIdentifier", "dbClusterIdentifier").to(AwsEntityType.AwsRdsInstance.name()).merge();
		
	}
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsRdsInstance;
	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected AmazonRDSClient getClient() {
		return getClient(AmazonRDSClientBuilder.class);
	}
}
