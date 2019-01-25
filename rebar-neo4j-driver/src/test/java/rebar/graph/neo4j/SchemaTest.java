package rebar.graph.neo4j;

import org.junit.jupiter.api.Test;

public class SchemaTest extends IntegrationTest {


	
	@Test
	public void testIdempotency() {
	
		String label = "JUnitIndex";
		String attr = "bar";
		getNeo4jDriver().cypher("match (a:JUnitIndex) detach delete a").exec(); // clear all items so that the create index cannot fail

		// call createUniqueIndex twice to verify idempotency
		getNeo4jDriver().schema().createUniqueConstraint(label, attr);
		getNeo4jDriver().schema().createUniqueConstraint(label, attr);
		
		// call dropUniqueIndex twice to verify idempotency
		getNeo4jDriver().schema().dropUniqueConstraint(label,attr);
		getNeo4jDriver().schema().dropUniqueConstraint(label,attr);
	}

}
