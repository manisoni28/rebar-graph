package rebar.graph.aws;

public enum AwsNodeType {

	AwsElb("AwsElb"),
	AwsVpc("AwsVpc"),
	AwsAccount("AwsAccount");
	
	private String name;
	AwsNodeType(String type) {
		this.name = type;
	}
	
	
}
