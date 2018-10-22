package rebar.graph.neo4j;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Usage extends Neo4jIntegrationTest {

	

	@Test
	@Disabled
	public void dummy() throws IOException {

		// Neo4jDriver driver = new Neo4jDriver.Builder().build();
		// Neo4jDriver driver = new Neo4jDriver.Builder().withCredentials("myusername",
		// "mypassword").build();

		
	/*	Driver bolt = GraphDatabase.driver(
				"bolt://neo4j.example.com", 
				AuthTokens.basic("myusername", "mypassword"));
		
		Neo4jDriver driver = new Neo4jDriver.Builder()
				.withDriver(bolt)
				.build();
		*/
		
		
		createMovieGraph();
		Neo4jDriver driver = getNeo4jDriver();
		
		JsonNode params = 
			    new ObjectMapper()
			    .createObjectNode()
			    .put("name","Rob Schoening")
			    .put("birthYear", 1975)
			    .put("occupation","Engineer");
			    
			driver.cypher("merge (a:Person {name:{name}}) set a={__params} return a")
				.params(params)
				.stream()
				.forEach(System.out::println);
	}

}
