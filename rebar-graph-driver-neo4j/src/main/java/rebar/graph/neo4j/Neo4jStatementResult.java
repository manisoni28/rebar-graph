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

import rebar.graph.driver.*;
import rebar.graph.driver.GraphTemplate.AttributeMode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Neo4jStatementResult {

	static Logger logger = LoggerFactory.getLogger(Neo4jStatementResult.class);

	Neo4jTemplate builder;

	Neo4jStatementResult(Neo4jTemplate builder) {
		this.builder = builder;
	}

	void consume(StatementResult sr) {

		sr.forEachRemaining(new StatementConsumer());
		sr.consume();
	}

	List<JsonNode> results = new ArrayList<>();

	public Stream<JsonNode> asStream() {
		return results.stream();
	}

	public List<JsonNode> asList() {
		return results;
	}

	class StatementConsumer implements java.util.function.Consumer<Record> {
		@Override
		public void accept(Record t) {

			boolean flatten = false;

			ObjectNode n = new ObjectMapper().createObjectNode();

			Map<String, Object> vals = t.asMap();

			if (builder.getAttributeMode() == GraphTemplate.AttributeMode.AUTO) {
				flatten = vals.size() == 1;
			} else if (builder.getAttributeMode() == AttributeMode.FLATTEN) {
				flatten = true;
			} else {
				flatten = false;
			}

			final boolean finalFlatten = flatten;
			vals.forEach((k, v) -> {

				if (v instanceof Entity) {
					Entity nn = Entity.class.cast(v);
					Map<String, Object> nm = nn.asMap();

					JsonNode cv = Neo4jDriver.mapper.valueToTree(nm);

					if (!finalFlatten) {
						n.set(k, cv);
					} else {
						cv.fields().forEachRemaining(x -> {
							if (vals.size() == 1) {
								n.set(x.getKey(), x.getValue());
							} else {
								n.set(k + "." + x.getKey(), x.getValue());
							}
						});
					}
				} else {
					if (v instanceof String) {
						n.put(k, (String) v);
					} else if (v instanceof Integer) {
						n.put(k, (Integer) v);
					} else if (v instanceof Long) {
						n.put(k, (Long) v);
					} else if (v instanceof BigDecimal) {
						n.put(k, (BigDecimal) v);
					} else if (v instanceof BigInteger) {
						n.put(k, (BigInteger) v);
					} else if (v instanceof Boolean) {
						n.put(k, (Boolean) v);
					} else if (v instanceof List) {
						n.set(k, Neo4jDriver.mapper.valueToTree(v));
					} else {
						logger.warn("unsupported type ({}): {}", k, v.getClass());
					}
				}

			});

			results.add(n);

		}
	}
}
