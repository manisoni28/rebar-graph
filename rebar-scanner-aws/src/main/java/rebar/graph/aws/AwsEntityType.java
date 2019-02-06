package rebar.graph.aws;

public enum AwsEntityType {

	
	AwsAccount("AwsAccount"),
	AwsAccountRegion("AwsAccount"),
	AwsAmi("AwsAmi"),
	AwsApiGatewayRestApi("AwsApiGatewayRestApi"),
	AwsAsg("AwsAsg"),
	AwsAvailabilityZone("AwsAvailabilityZone"),
	AwsCacheCluster("AwsCacheCluster"),
	AwsCacheClusterNode("AwsCacheClusterNode"),
	AwsEc2Instance("AwsEc2Instance"),
	AwsEgressOnlyInternetGateway("AwsEgressOnlyInternetGateway"),
	AwsEksCluster("AwsEksCluster"),
	AwsEmrCluster("AwsEmrCluster"),
	AwsEmrClusterInstance("AwsEmrClusterInstance"),
	AwsElb("AwsElb"),
	AwsElbTargetGroup("AwsElbTargetGroup"),
	
	AwsHostedZone("AwsHostedZone"),
	AwsHostedZoneRecordSet("AwsHostedZoneRecordSet"),
	AwsIamUser("AwsIamUser"),
	AwsIamPolicy("AwsIamPolicy"),
	AwsIamInstanceProfile("AwsIamInstanceProfile"),
	AwsIamRole("AwsIamRole"),
	AwsInternetGateway("AwsInternetGateway"),
	AwsLambdaFunction("AwsLambdaFunction"),
	AwsLaunchConfig("AwsLaunchConfig"),
	AwsLaunchTemplate("AwsLaunchTemplate"),
	AwsRdsCluster("AwsRdsCluster"),
	AwsRdsInstance("AwsRdsInstance"),
	AwsRegion("AwsRegion"),
	AwsRouteTable("AwsRouteTable"),
	AwsSecurityGroup("AwsSecurityGroup"),
	AwsSnsTopic("AwsSnsTopic"),
	AwsSnsSubscription("AwsSnsSubscription"),
	AwsSqsQueue("AwsSqsQueue"),
	AwsElbListener("AwsElbListener"),
	AwsS3Bucket("AwsS3Bucket"),
	AwsSubnet("AwsSubnet"),
	AwsVpc("AwsVpc"),
	AwsVpcEndpoint("AwsVpcEndpoint"),
	AwsVpcPeeringConnection("AwsVpcPeeringConnection"),
	AwsVpnGateway("AwsVpnGateway"),
	UNKNOWN("UNKNOWN");
	
	private String name;
	AwsEntityType(String type) {
		this.name = type;
	}
	
	
}
