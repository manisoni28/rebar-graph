package rebar.graph.docker;

public enum DockerEntityType {

	DockerContainer("DockerContainer"),
	DockerHost("DockerHost"),
	DockerImage("DockerImage");
	
	
	private String name;
	
	DockerEntityType(String type) {
		this.name = type;
	}
	
}
