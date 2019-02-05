package rebar.graph.azure;

public enum AzureEntityType {
	AzureSubscription("AzureScubscription");
	private String name;
	
	AzureEntityType(String type) {
		this.name = type;
	}
	
}
