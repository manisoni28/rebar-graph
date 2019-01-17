/**
 * Copyright 2018-2019 Rob Schoening
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
package rebar.graph.core;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.neo4j.GraphDriver;
import rebar.util.Json;

public class RebarGraphTest extends Neo4jIntegrationTest {

	@org.junit.jupiter.api.Test
	public void testIt() {

		RebarGraph graph = getRebarGraph();

		Assertions.assertThat(graph).isNotNull();
		Assertions.assertThat(graph.getGraphDB()).isNotNull().isSameAs(graph.getGraphDB());

		GraphDriver driver = ((GraphDB) graph.getGraphDB()).getNeo4jDriver();

		Assertions.assertThat(driver).isSameAs(((GraphDB) graph.getGraphDB()).getNeo4jDriver());

		Assertions.assertThat(driver.getDriver()).isSameAs(driver.getDriver());

		graph.getGraphDB().nodes("JUnitPerson").idKey("name").property("name", "Rob")
				.property("occupation", "developer").merge();
		
	
		graph.getGraphDB().nodes("JUnitPerson").idKey("name").property("name", "Rob")
		.property("occupation", "developer").merge().forEach(it->{
			System.out.println(it);
		});

	}
	@org.junit.jupiter.api.Test
	public void testRelationship() {

		RebarGraph graph = getRebarGraph();
		GraphDB db = graph.getGraphDB();
		
		db.nodes("TestFrom").property("name", "a").idKey("name").merge();
		db.nodes("TestFrom").property("name", "b").idKey("name").merge();
		
		db.nodes("TestTo").property("name", "x").idKey("name").merge();
		db.nodes("TestTo").property("name", "y").idKey("name").merge();
		db.nodes("TestFrom").match().forEach(Json.logger()::info);
		
		db.nodes("TestFrom").id( "name","a").relationship("HAS").to("TestTo").id( "name","y").merge();
		
		
		getNeo4jDriver().cypher("match (a:TestFrom)--(b:TestTo) return a,b").stream().forEach(Json.logger()::info);
		
		
	}

	@Test
	public void testIt4() {
		getRebarGraph().getGraphDB().nodes("TestFoo").property("foo", "bar").property("fizz", "buzz")
				.idKey("foo").merge().forEach(it -> {
					Assertions.assertThat(it.path(GraphDB.UPDATE_TS).asLong()).isCloseTo(System.currentTimeMillis(),
							Offset.offset(1000L));
					Assertions.assertThat(it.path("foo").asText()).isEqualTo("bar");
					Assertions.assertThat(it.path("fizz").asText()).isEqualTo("buzz");
				});
		getRebarGraph().getGraphDB().nodes("TestFoo").property("foo", "bar").property("fizz", "buzzbuzz")
				.idKey("foo").merge().forEach(it -> {
					Assertions.assertThat(it.path(GraphDB.UPDATE_TS).asLong()).isCloseTo(System.currentTimeMillis(),
							Offset.offset(1000L));
					Assertions.assertThat(it.path("foo").asText()).isEqualTo("bar");
					Assertions.assertThat(it.path("fizz").asText()).isEqualTo("buzzbuzz");
				});

		Assertions.assertThat(getRebarGraph().getGraphDB().nodes("TestFoo").property("foo", "bar")
				.property("fizz", "buzzbuzz").idKey("foo").removeProperties("abba", "zabba").match()
				.count()).isEqualTo(1);
	}

	@Test
	public void testIt3() {
		getRebarGraph().getGraphDB().nodes("TestBar").property("foo", "bar").property("fizz", "buzz")
				.idKey("foo").merge();
		getRebarGraph().getGraphDB().nodes("TestBar").property("foo", "bop").property("fizz", "fuzz")
				.idKey("foo").merge();

		Assertions.assertThat(getRebarGraph().getGraphDB().nodes("TestBar").match().count()).isEqualTo(2);

		Assertions.assertThat(getRebarGraph().getGraphDB().nodes("TestBar").id("foo", "bop")
				.match().findFirst().get().path("fizz").asText()).isEqualTo("fuzz");
		
		Assertions.assertThat(getRebarGraph().getGraphDB().nodes("TestBar").id("nope", "nope")
				.match().findFirst().isPresent()).isFalse();
	}

	@Test
	public void testIt2() {

		getRebarGraph().getGraphDB().nodes("TestFoo").property("foo", "bar").property("fizz", "buzz")
				.idKey("foo").merge().forEach(it -> {
					Assertions.assertThat(it.path(GraphDB.UPDATE_TS).asLong()).isCloseTo(System.currentTimeMillis(),
							Offset.offset(1000L));
					Assertions.assertThat(it.path("foo").asText()).isEqualTo("bar");
					Assertions.assertThat(it.path("fizz").asText()).isEqualTo("buzz");
				});

		Assertions.assertThat(getRebarGraph().getGraphDB().nodes("TestFoo").match().count()).isEqualTo(1);

		getRebarGraph().getGraphDB().nodes("TestFoo").delete();

		Assertions.assertThat(getRebarGraph().getGraphDB().nodes("TestFoo").match().count()).isEqualTo(0);

	}
	
	@Test
	public void testX() {
		
		getRebarGraph().getGraphDB().nodes("TestFoo").id("name", "a").merge().forEach(Json.logger()::info);
		getRebarGraph().getGraphDB().nodes("TestFoo").id("name", "b").merge().forEach(Json.logger()::info);
		getRebarGraph().getGraphDB().nodes("TestBar").id("name", "x").merge().forEach(Json.logger()::info);
		getRebarGraph().getGraphDB().nodes("TestBar").id("name", "y").merge().forEach(Json.logger()::info);
		
		getRebarGraph().getGraphDB().nodes("TestFoo").id("name", "a").relationship("HAS").to("TestBar").id("name", "x").merge();
		
		Assertions.assertThat(getNeo4jDriver().cypher("match (a:TestFoo)--(b:TestBar) return a,b").stream().count()).isEqualTo(1);
		
		getRebarGraph().getGraphDB().nodes("TestFoo").id("name", "b").relationship("HAS").to("TestBar").id("name", "y").merge();
		
		
		Assertions.assertThat(getNeo4jDriver().cypher("match (a:TestFoo)--(b:TestBar) return a,b").stream().count()).isEqualTo(2);
		
	//	Assertions.assertThat(getNeo4jDriver().cypher("match (a:TestFoo)-[r:HAS]->(b:TestBar) detach delete r return r").stream().count()).isEqualTo(2);
		
	
		
	//	Assertions.assertThat(getRebarGraph().getGraphDB().nodes("TestFoo").match().count()).isEqualTo(2);
		
	//	Assertions.assertThat(getRebarGraph().getGraphDB().nodes("TestBar").match().count()).isEqualTo(2);
		
	//	Assertions.assertThat(getNeo4jDriver().cypher("match (a:TestFoo)--(b:TestBar) return a,b").stream().count()).isEqualTo(0);
		
		
	}
}
