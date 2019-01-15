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
package rebar.graph.core;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import rebar.graph.driver.GraphException;
import rebar.util.Json;

class Neo4jGraphDBTest extends Neo4jIntegrationTest {

	@Test
	public void testIt() {

		Assertions.assertThat(getRebarGraph()).isNotNull();

		getRebarGraph().getGraphDB().nodes().label("JUnitFoo")
				.properties(Json.objectNode().put("name", "rob " + System.currentTimeMillis()).put("fizz", 123)).idKey("name").merge()
				.forEach(it -> {
					System.out.println(">>> " + it);

					Assertions.assertThat(it.path("fizz").asInt()).isEqualTo(123);
					Assertions.assertThat(it.path("name").asText()).startsWith("rob ");
					Assertions.assertThat(it.path("graphUpdateTs").asLong()).isCloseTo(System.currentTimeMillis(),
							Offset.offset(1000L));
				});
	}

	@Test
	public void testKeyNotFound() {

		Assertions.assertThat(getRebarGraph()).isNotNull();

		try {
			getRebarGraph().getGraphDB().nodes().label("JUnitFoo")
					.properties(Json.objectNode().put("name", "rob " + System.currentTimeMillis())).idKey("NOTFOUND")
					.merge().forEach(it -> {

					});
			Assertions.failBecauseExceptionWasNotThrown(GraphException.class);
		} catch (Exception e) {
			Assertions.assertThat(e).isInstanceOf(GraphException.class).hasMessageContaining("NOTFOUND");
		}
	}

	@Test
	public void testTimestamp() {
		Assertions.assertThat(getRebarGraph().getGraphDB().getTimestamp()).isCloseTo(System.currentTimeMillis(),
				Offset.offset(5000L));
	}

	@Test
	public void testPattern() {

		Assertions
				.assertThat(Neo4jGraphDB.toPatternClause(ImmutableMap.of("name", "Rob", "occupation", "developer")))
				.contains("name:{name}").contains("occupation:{occupation}");
	}
}
