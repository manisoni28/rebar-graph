package rebar.graph.test;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import rebar.graph.core.BaseConfig;

public class Neo4jAvailabilityTest extends AbstractIntegrationTest {


	@Test
	public void testNeo4jCheck() {
		// this test should NOT fail if neo4j is unreachable.  It should be skipped.
		getGraphDriver().cypher("match (a:FizzBuzz) return a").findFirst();
	}

}
