package rebar.graph.catalog;

public enum CatalogEntityType {

	ServiceCatalogEntry("ServiceCatalogEntry"),
	DatabaseCatalogEntry("DatabaseCatalogEntry"),
	StreamCatalogEntry("StreamCatalogEntry"),
	QueueCatalogEntry("QueueCatalogEntry");
	
	private String name;
	
	CatalogEntityType(String type) {
		this.name = type;
	}
	
	
}
