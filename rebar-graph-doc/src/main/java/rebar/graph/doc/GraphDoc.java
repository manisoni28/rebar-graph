package rebar.graph.doc;

import java.io.File;
import java.io.IOException;

import rebar.graph.neo4j.Neo4jDriver;

public class GraphDoc {

	
	
	public static void main(String [] args) throws IOException {
		DataModelMarkdown dm2 = new DataModelMarkdown().parse(new File("../rebar-graph-aws/README.md")).withNeo4jDriver(new Neo4jDriver.Builder().build());
		dm2.mergeAll(p->p.startsWith("Aws")).write();
	
		
		dm2 = new DataModelMarkdown().parse(new File("../rebar-graph-kubernetes/README.md")).withNeo4jDriver(new Neo4jDriver.Builder().build());
		dm2.mergeAll(p->p.startsWith("Kube")).write();
	
	}

}
