package rebar.graph.digitalocean;

public enum DigitalOceanEntityType {

	DigitalOceanAccount("DigitalOceanAccount"),
	DigitalOceanRegion("DigitalOceanRegion"),
	DigitalOceanDroplet("DigitalOceanDroplet");
	
	private String name;
	
	DigitalOceanEntityType(String type) {
		this.name = type;
	}
}
