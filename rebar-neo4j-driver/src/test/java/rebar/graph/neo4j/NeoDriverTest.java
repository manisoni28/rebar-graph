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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import rebar.util.Json;

public class NeoDriverTest extends IntegrationTest {

	static ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testMovieGraph() {
		createMovieGraph();

		Set<String> names = getNeo4jDriver()
				.cypher("match (p:Person)-[r:ACTED_IN]-(x:Movie {title:{movieName}}) return p,type(r),x")
				.param("movieName", "The Matrix").stream().map(it -> it.path("p").path("name").asText())
				.collect(Collectors.toSet());

		Assertions.assertThat(names).contains("Hugo Weaving", "Carrie-Anne Moss").doesNotContain("Joel Silver");

		names = getNeo4jDriver()
				.cypher("match (p:Person)-[r:PRODUCED]-(x:Movie {title:{movieName}}) return p,type(r),x")
				.withAttributeMode(rebar.graph.neo4j.CypherTemplate.AttributeMode.FLATTEN).param("movieName", "The Matrix").stream()
				.map(it -> it.path("p.name").asText()).collect(Collectors.toSet());

		Assertions.assertThat(names).doesNotContain("Hugo Weaving", "Carrie-Anne Moss").contains("Joel Silver");

	}

	@Test
	public void testInsert() {

		List<String> test = new ArrayList<>();
		test.add("a");
		test.add("b");
		JsonNode n = getNeo4jDriver().cypher("create (a:JUnitFoo {name:'rob'}) set a.list={list} return a")
				.param("list", test).stream().findFirst().get();

		Assertions.assertThat(n.path("name").asText()).isEqualTo("rob");
		Assertions.assertThat(n.path("list").get(0).asText()).isEqualTo("a");
		Assertions.assertThat(n.path("list").get(1).asText()).isEqualTo("b");

		Assertions.assertThat(
				getNeo4jDriver().cypher("match (a:JUnitFoo {name:'rob'}) set a.list={list} detach delete a return a")
						.param("list", test).stream().count())
				.isEqualTo(1);

		Assertions.assertThat(
				getNeo4jDriver().cypher("match (a:JUnitFoo {name:'rob'}) set a.list={list} detach delete a return a")
						.param("list", test).stream().count())
				.isEqualTo(0);
	}

	private GraphDriver.Builder applyCredentials(GraphDriver.Builder b) {
		if (getTestProperty(GraphDriver.GRAPH_USERNAME).isPresent()) {
			b = b.withUsername(getTestProperty(GraphDriver.GRAPH_USERNAME).get());
		}
		if (getTestProperty(GraphDriver.GRAPH_PASSWORD).isPresent()) {
			b = b.withPassword(getTestProperty(GraphDriver.GRAPH_PASSWORD).get());
		}
		return b;
	}

	@Test
	public void testGraphBuilder() {

		// We don't close the driver here because it is slow
		Neo4jDriverImpl driver = (Neo4jDriverImpl) applyCredentials(new Neo4jDriverImpl.Builder().withEnv("GRAPH_URL", getUrl()))
				.build();
		driver.newTemplate().cypher("match (a:JUnitTest) return a limit 1").exec();

		driver = (Neo4jDriverImpl) applyCredentials(new GraphDriver.Builder().withUrl(getUrl())).build();
		driver.newTemplate().cypher("match (a:JUnitTest) return a limit 1").exec();

		driver = (Neo4jDriverImpl) applyCredentials(new GraphDriver.Builder().withEnv("GRAPH_URL", getUrl())).build();
		driver.newTemplate().cypher("match (a:JUnitTest) return a limit 1").exec();

	}

	@Test
	public void testMovie() {
		createMovieGraph();

	}

	@Test
	public void testIt() {

		Map<String, String> m = new HashMap<>();
		m.put("name", "Rob");
		List<JsonNode> results = getNeo4jDriver().newTemplate()
				.cypher("merge (a:JunitFoo {name:{name}}) return a.name as name").params(m).list();

		results.forEach(it -> {
			System.out.println(it);
		});

		ObjectNode foo = getNeo4jDriver().mapper.createObjectNode();
		foo.put("name", "Rob").put("age", 43);

		foo = getNeo4jDriver().mapper.createObjectNode();
		foo.put("name", "Homer").put("age", 1);

		getNeo4jDriver().newTemplate().cypher("match (a:Hello) return a").list().forEach(it -> {
			System.out.println(it);
		});
	}

	@Test
	public void testMap() {
		String id = UUID.randomUUID().toString();
		Map<String, Object> data = Maps.newHashMap();
		data.put("foo", "bar");
		data.put("fizz", "buzz");
		data.put("abc", 123);
		JsonNode result = getNeo4jDriver().cypher("merge (f:JUnit {id:{id}}) set f+={data} return f").param("id", id)
				.param("data", data).findFirst().get();

		Assertions.assertThat(result.path("id").asText()).isEqualTo(id);
		Assertions.assertThat(result.path("foo").asText()).isEqualTo("bar");
		Assertions.assertThat(result.path("fizz").asText()).isEqualTo("buzz");
		Assertions.assertThat(result.path("abc").asInt()).isEqualTo(123);
	}

	@Test
	public void testJsonArg() {
		String id = UUID.randomUUID().toString();
		ObjectNode data = Json.objectNode();
		data.put("foo", "bar");
		data.put("fizz", "buzz");
		data.put("abc", 123);
		JsonNode result = getNeo4jDriver().cypher("merge (f:JUnit {id:{id}}) set f+={data} return f").param("id", id)
				.param("data", data).findFirst().get();

		Assertions.assertThat(result.path("id").asText()).isEqualTo(id);
		Assertions.assertThat(result.path("foo").asText()).isEqualTo("bar");
		Assertions.assertThat(result.path("fizz").asText()).isEqualTo("buzz");
		Assertions.assertThat(result.path("abc").asInt()).isEqualTo(123);
	}

	@Test
	public void testList() {
		String id = UUID.randomUUID().toString();

		String[] arr = { "a", "b", "c" };
		List<String> list = ImmutableList.of("x", "y", "z");
		JsonNode result = getNeo4jDriver().cypher("merge (f:JUnit {id:{id}}) set f.list={list},f.array={array} return f")
				.param("id", id).param("array", arr).param("list", list).findFirst().get();

		Assertions.assertThat(Json.objectMapper().convertValue(result.path("array"),String[].class)).containsExactly("a","b","c");
		
		Assertions.assertThat(Json.objectMapper().convertValue(result.path("list"),Object[].class)).containsExactly("x","y","z");
	}
}
