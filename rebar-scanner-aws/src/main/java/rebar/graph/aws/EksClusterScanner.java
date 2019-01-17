package rebar.graph.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.eks.AmazonEKS;
import com.amazonaws.services.eks.AmazonEKSClientBuilder;
import com.amazonaws.services.eks.model.Cluster;
import com.amazonaws.services.eks.model.DescribeClusterRequest;
import com.amazonaws.services.eks.model.DescribeClusterResult;
import com.amazonaws.services.eks.model.ListClustersRequest;
import com.amazonaws.services.eks.model.ListClustersResult;
import com.amazonaws.services.eks.model.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.util.Json;

public class EksClusterScanner extends AbstractEntityScanner<Cluster> {

	

	@Override
	protected void doScan() {
		long ts = System.currentTimeMillis();
		AmazonEKS eks = getClient(AmazonEKSClientBuilder.class);

		ListClustersRequest request = new ListClustersRequest();
		do {
			ListClustersResult result = eks.listClusters(request);
			result.getClusters().forEach(cluster -> {
				tryExecute(() -> scan(cluster));
			});

			request.setNextToken(result.getNextToken());
		} while (!Strings.isNullOrEmpty(request.getNextToken()));


		gc("AwsEksCluster", ts);
		mergeSecurityGroupRelationships();
		mergeSubnetRelationships();

	}

	@Override
	protected ObjectNode toJson(Cluster awsObject) {
		ObjectNode n = super.toJson(awsObject);
		
		
		n.set("subnets", n.path("resourcesVpcConfig").path("subnetIds"));
		n.set("securityGroups", n.path("resourcesVpcConfig").path("securityGroupIds"));
		n.set("vpcId", n.path("resourcesVpcConfig").path("vpcId"));
		n.remove("resourcesVpcConfig");
		n.set("certificateAuthorityData", n.path("certificateAuthority").path("data"));
		n.remove("certificateAuthority");
	
		return n;
	}

	protected void project(Cluster cluster) {
		JsonNode n = toJson(cluster);

		awsGraphNodes("AwsEksCluster").idKey("arn").properties(n).merge();

	}

	@Override
	public void scan(JsonNode entity) {
		scan(entity.path("name").asText());
	}


	
	@Override
	public void scan(String clusterName) {
		try {
			AmazonEKS eks = getClient(AmazonEKSClientBuilder.class);

			DescribeClusterResult result = eks.describeCluster(new DescribeClusterRequest().withName(clusterName));

			project(result.getCluster());
			
			mergeSecurityGroupRelationships();
			mergeSubnetRelationships();
			

		} catch (ResourceNotFoundException e) {
			awsGraphNodes("AwsEksCluster").id("name", clusterName).delete();
		}

	}

}
