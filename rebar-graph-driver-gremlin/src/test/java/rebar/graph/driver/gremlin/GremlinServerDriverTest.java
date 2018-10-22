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
package rebar.graph.driver.gremlin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GremlinServerDriverTest extends GremlinServerTest {

	@Test
	public void testProps() {
		// getGremlinDriver().gremlin(g->g.V()).delete();
		Vertex v = getGremlinDriver().mergeVertex("Foo", "a", 1);

		getGremlinDriver().gremlin(g -> {
			return g.V().has("q", 42).property("x", Stream.of(1, 2, 4).collect(Collectors.toList()));
		}).exec();

	}

	@Test
	public void testMergeEdge() {

		getGremlinDriver().gremlin(g -> g.V()).delete();
		Vertex v1 = getGremlinDriver().mergeVertex("Foo", "a", 1);
		Vertex v2 = getGremlinDriver().mergeVertex("Bar", "b", 2);

		Assertions.assertThat(getGremlinDriver().mergeEdge(v1, "HAS", v2).get().id())
				.isEqualTo(getGremlinDriver().mergeEdge(v1, "HAS", v2).get().id());

		getGremlinDriver().gremlin(g -> g.V()).delete();

		Assertions.assertThat(getGremlinDriver().mergeEdge(v1, "HAS", v2)).isNotPresent();

	}

	@Test
	public void testMergeEdges() {

		getGremlinDriver().gremlin(g -> g.V()).delete();
		Vertex v1 = getGremlinDriver().mergeVertex("Foo", "a", 1);
		Vertex v2 = getGremlinDriver().mergeVertex("Bar", "b", 2);

		List<Edge> xx = getGremlinDriver().mergeEdges(v1, "HAS", "Bar", "b", 2);

		xx.forEach(it -> {
			System.out.println(it);
		});

	}

	@Test
	public void testMergeVertex() {

		getGremlinDriver().gremlin(g -> g.V()).delete();
		Assertions.assertThat(getGremlinDriver().mergeVertex("JUnitFizz", "a", "1").id())
				.isEqualTo(getGremlinDriver().mergeVertex("JUnitFizz", "a", "1").id());
		Assertions.assertThat(getGremlinDriver().mergeVertex("JUnitFizz", "a", "1").id())
				.isNotEqualTo(getGremlinDriver().mergeVertex("JUnitFizz", "a", "2").id());
		Assertions.assertThat(getGremlinDriver().mergeVertex("JUnitFizz", "a", "1").id())
				.isNotEqualTo(getGremlinDriver().mergeVertex("JUnitBuzz", "a", "1").id());

		Vertex v = getGremlinDriver().mergeVertex("JUnitFizz", "q", "42");
		Vertex v2 = getGremlinDriver().mergeVertex("JUnitFizz", "q", 42);
		Assertions.assertThat(v.id()).isNotEqualTo(v2.id());

	}

	@Test
	public void testIt() throws JsonProcessingException {

		ObjectMapper mapper = new ObjectMapper();

		/*
		 * TinkerGraph tg = TinkerFactory.createModern();
		 * tg.traversal().addV("Foo").property("a", "b").tryNext();
		 * tg.traversal().V().forEachRemaining(it->{
		 * System.out.println(it.label()+" "+it.keys()); });
		 */

		GraphTraversalSource gt = getGremlinDriver().traversal();

		Assertions.assertThat(getGremlinDriver().mergeVertex("Fizz", "a", "1").id())
				.isEqualTo(getGremlinDriver().mergeVertex("Fizz", "a", "1").id());
		Assertions.assertThat(getGremlinDriver().mergeVertex("Fizz", "a", "1").id())
				.isNotEqualTo(getGremlinDriver().mergeVertex("Buzz", "a", "2").id());
		Vertex v2 = getGremlinDriver().mergeVertex("B", "b", "bval");
		Vertex v = getGremlinDriver().mergeVertex("A", "a", "aval");

		gt.addE("HAS").from(v).to(v2).iterate();

		// getGremlinDriver().gremlin(g->g.V().valueMap(true)).asStream().forEach(System.out::println);

		getGremlinDriver().gremlin(g -> g.V().as("x").outE("HAS").inV().as("y").select("x", "y").by(__.valueMap(false)))
				.forEach(it -> {

				});

		gt.V().as("x").outE("HAS").inV().as("y").select("x").forEachRemaining(it -> {
			// System.out.println("___");
			// System.out.println(it);
		});

		getGremlinDriver().gremlin(g -> g.V()).delete();

	}

}
