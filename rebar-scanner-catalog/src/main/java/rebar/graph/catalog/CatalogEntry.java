package rebar.graph.catalog;

import com.fasterxml.jackson.databind.JsonNode;

public interface CatalogEntry {

	String getName();
	JsonNode getData();
	CatalogEntityType getType();
}
