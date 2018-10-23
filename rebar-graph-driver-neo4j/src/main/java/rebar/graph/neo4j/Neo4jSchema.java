package rebar.graph.neo4j;

import rebar.graph.driver.GraphSchema;

public class Neo4jSchema implements GraphSchema {

	Neo4jDriver driver;
	public Neo4jSchema(Neo4jDriver driver) {
		this.driver = driver;
	}

	@Override
	public void ensureUniqueIndex(String label, String attribute) {
		
		driver.cypher("CREATE CONSTRAINT ON (x:"+label+") ASSERT x.`"+attribute+"` IS UNIQUE").exec();

	}

}
