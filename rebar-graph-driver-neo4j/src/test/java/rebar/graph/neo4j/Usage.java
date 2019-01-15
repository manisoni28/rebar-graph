/**
 * Copyright 2018 Rob Schoening
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

public class Usage extends IntegrationTest {

	

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
