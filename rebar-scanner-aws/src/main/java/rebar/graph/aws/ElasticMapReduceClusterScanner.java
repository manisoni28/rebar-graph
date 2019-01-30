package rebar.graph.aws;

import java.util.ArrayList;
import java.util.Optional;

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder;
import com.amazonaws.services.elasticmapreduce.model.Cluster;
import com.amazonaws.services.elasticmapreduce.model.ClusterSummary;
import com.amazonaws.services.elasticmapreduce.model.DescribeClusterRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeClusterResult;
import com.amazonaws.services.elasticmapreduce.model.Ec2InstanceAttributes;
import com.amazonaws.services.elasticmapreduce.model.InvalidRequestException;
import com.amazonaws.services.elasticmapreduce.model.ListClustersRequest;
import com.amazonaws.services.elasticmapreduce.model.ListClustersResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import rebar.util.Json;

public class ElasticMapReduceClusterScanner extends AwsEntityScanner<Cluster> {

	protected AmazonElasticMapReduceClient getClient() {
		return getAwsScanner().getClient(AmazonElasticMapReduceClientBuilder.class);
	}

	@Override
	protected void doScan() {
		AmazonElasticMapReduce emr = getClient();
		long ts = getGraphDB().getTimestamp();
		ListClustersRequest request = new ListClustersRequest();
		do {
			ListClustersResult result = emr.listClusters(request);
			result.getClusters().forEach(it -> {
				tryExecute(() -> {
					project(it);
					doScan(it.getId());
				});
			});
		} while (!Strings.isNullOrEmpty(request.getMarker()));
		mergeAccountOwner();

		gc(AwsEntityType.AwsEmrCluster.name(), ts);
	}

	public Optional<String> toArn(ClusterSummary cluster) {

		return Optional.ofNullable(String.format("arn:aws:elasticmapreduce:%s:%s:cluster/%s", getRegionName(),
				getAccount(), cluster.getId()));
	}
	public Optional<String> toArn(Cluster cluster) {

		return Optional.ofNullable(String.format("arn:aws:elasticmapreduce:%s:%s:cluster/%s", getRegionName(),
				getAccount(), cluster.getId()));
	}
	protected void project(Cluster cluster) {
		ObjectNode n = toJson(cluster);
		
		awsGraphNodes(AwsEntityType.AwsEmrCluster.name()).idKey("arn").properties(n).merge();
		
	}
	
	protected ObjectNode toJson(Cluster cluster) {
		ObjectNode n = super.toJson(cluster);
		n.set("state", n.path("status").path("state"));
		n.set("stateChangeReasonCode", n.path("status").path("stateChangeReason").path("code"));
		n.set("stateChangeReasonMessage", n.path("status").path("stateChangeReason").path("message"));
		n.set("creationTs", n.path("status").path("timeline").path("creationDateTime"));
		n.set("readyTs", n.path("status").path("timeline").path("readyDateTime"));
		n.set("endTs", n.path("status").path("timeline").path("endDateTime"));
		n.remove("status");
		
		
		n.put("kerberosRealm", cluster.getKerberosAttributes().getRealm());
		n.put("kerberosKdcAdminPassword", cluster.getKerberosAttributes().getKdcAdminPassword());
		n.put("kerberosADDomainJoinUser",cluster.getKerberosAttributes().getADDomainJoinUser());
		n.put("kerberosADDomainJoinPasswordr",cluster.getKerberosAttributes().getADDomainJoinPassword());
		n.put("kerberosCrossRealmTrustPrincipalPassword",cluster.getKerberosAttributes().getCrossRealmTrustPrincipalPassword());
		n.remove("kerberosAttributes");
		
		
		ArrayNode appList = Json.arrayNode();
		n.path("applications").forEach(it->{
			String name = it.path("name").asText().toLowerCase();
			appList.add(name);
			n.set(name+"Version",it.path("version"));
			n.set(name+"Args",it.path("args"));
			n.set(name+"AdditionalInfo",it.path("additionalInfo"));
			
		});
		n.set("applicationNames", appList);
		n.remove("applications");
		
		Ec2InstanceAttributes ec2 = cluster.getEc2InstanceAttributes();
		n.put("ec2AvailabilityZone",ec2.getEc2AvailabilityZone());
		n.put("ec2KeyName",ec2.getEc2KeyName());
		n.put("ec2SubnetId",ec2.getEc2SubnetId());
		n.set("ec2RequestedSubnetIds", Json.objectMapper().valueToTree(ec2.getRequestedEc2SubnetIds()));
		n.set("ec2RequestedAvailabilityZones", Json.objectMapper().valueToTree(ec2.getRequestedEc2AvailabilityZones()));
		n.put("ec2IamInstanceProfile",ec2.getEc2SubnetId());
		n.put("ec2EmrManagedMasterSecurityGroup",ec2.getEmrManagedSlaveSecurityGroup());
		n.put("ec2EmrManagedSlaveSecurityGroup",ec2.getEmrManagedSlaveSecurityGroup());
		n.set("ec2AdditionalMasterSecurityGroups",Json.objectMapper().valueToTree(ec2.getAdditionalMasterSecurityGroups()));
		n.set("ec2AdditionalSlaveSecurityGroups",Json.objectMapper().valueToTree(ec2.getAdditionalMasterSecurityGroups()));
		n.remove("ec2InstanceAttributes");
		return n;
	}
	protected void project(ClusterSummary summary) {
		ObjectNode n = (ObjectNode) Json.objectMapper().valueToTree(summary);
		n.put("account", getAccount());
		n.put("region", getRegionName());
		n.put("arn", toArn(summary).get());
		n.put("graphEntityType", AwsEntityType.AwsEmrCluster.name());
		n.put("graphEntityGroup", "aws");
		n.set("state", n.path("status").path("state"));
		n.set("stateChangeReasonCode", n.path("status").path("stateChangeReason").path("code"));
		n.set("stateChangeReasonMessage", n.path("status").path("stateChangeReason").path("message"));
		n.set("creationTs", n.path("status").path("timeline").path("creationDateTime"));
		n.set("readyTs", n.path("status").path("timeline").path("readyDateTime"));
		n.set("endTs", n.path("status").path("timeline").path("endDateTime"));
		n.remove("status");

		awsGraphNodes(AwsEntityType.AwsEmrCluster.name()).idKey("arn").properties(n).merge();

	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityOwner(entity)) {
			String id = entity.path("id").asText();
			doScan(id);
		}

	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		Preconditions.checkArgument(!id.startsWith("arn:"),"arn not yet supported");
		try {
			DescribeClusterResult result = getClient().describeCluster(new DescribeClusterRequest().withClusterId(id));
			project(result.getCluster());
		} catch (InvalidRequestException e) {
			if (e.getEmrErrorCode().equals("MalformedClusterId")) {
				deleteClusterId(id);
			} else {
				throw e;
			}

		}

	}

	private void deleteClusterId(String id) {
		awsGraphNodes(AwsEntityType.AwsEmrCluster.name()).id("id",id).delete();
	}
	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsEmrCluster;
	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}
}
