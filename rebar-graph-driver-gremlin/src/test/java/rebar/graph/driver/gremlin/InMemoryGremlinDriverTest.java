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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class InMemoryGremlinDriverTest extends InMemoryTest {

	static ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testIt() {

		
		Assertions.assertThat(getGremlinDriver()).isSameAs(getGremlinDriver());
		Assertions.assertThat(getGremlinDriver().traversal().getGraph()).isSameAs(getGremlinDriver().traversal().getGraph());

		getGremlinDriver().gremlin(g->g.V().hasLabel("person").as("p").outE().as("r").inV().as("x").select("p","r","x")).forEach(it->{
			System.out.println(it.path("p").path("name").asText()+" "+it.path("r").path("label").asText()+" "+it.path("x").path("name").asText());
		});
		
		
		getGremlinDriver().gremlin(g->{
			return g.V();
		}).delete();
	
		Assertions.assertThat(getGremlinDriver().gremlin(g->{
			return g.V().count();
		}).findFirst().get().asInt()).isEqualTo(0);
		
		/*
		Vertex v = getGremlinDriver().mergeVertex("Foo", "name","rob");
		
		getGremlinDriver().gremlin(g->{
			return g.addV("JUnitFoo").property("a", 1).property("b", Stream.of
					(1,2).collect(Collectors.toList())).valueMap();
		}).asList();
		
		
		getGremlinDriver().gremlin(g->g.V()).asList().forEach(it->{
			System.out.println(it);
		});
		*/
	}

}
