package rebar.graph.aws;

public enum AwsEntityType {

	
	AwsAccount("AwsAccount"),
	AwsAmi("AwsAmi"),
	AwsAsg("AwsAsg"),
	AwsAvailabilityZone("AwsAvailabilityZone"),
	AwsEc2Instance("AwsEc2Instance"),
	AwsEksCluster("AwsEksCluster"),
	AwsElb("AwsElb"),
	AwsElbTargetGroup("AwsElbTargetGroup"),
	AwsHostedZone("AwsHostedZone"),
	AwsHostedZoneRecordSet("AwsHostedZoneRecordSet"),
	AwsLambdaFunction("AwsLambdaFunction"),
	AwsLaunchConfig("AwsLaunchConfig"),
	AwsLaunchTemplate("AwsLaunchTemplate"),
	AwsRdsCluster("AwsRdsCluster"),
	AwsRdsInstance("AwsRdsInstance"),
	AwsRegion("AwsRegion"),
	AwsSecurityGroup("AwsSecurityGroup"),
	AwsElbListener("AwsElbListener"),
	AwsSubnet("AwsSubnet"),
	AwsVpc("AwsVpc"),
	UNKNOWN("UNKNOWN");
	
	private String name;
	AwsEntityType(String type) {
		this.name = type;
	}
	
	
}
