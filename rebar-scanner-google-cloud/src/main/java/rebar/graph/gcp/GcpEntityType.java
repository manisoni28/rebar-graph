package rebar.graph.gcp;

public enum GcpEntityType {
	GcpRegion("GcpRegion"),
	GcpZone("GcpZone"),
	GcpProject("GcpProject"),
	GcpComputeInstance("GcpComputeInstance"),
	GcpComputeImage("GcpComputeImage");
	private String name;
	
	GcpEntityType(String type) {
		this.name = type;
	}
	
}
