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
package rebar.graph.neo4j;

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

public class Neo4jStatementResultImpl {

	static Logger logger = LoggerFactory.getLogger(Neo4jStatementResultImpl.class);

	Neo4jTemplateImpl builder;

	Neo4jStatementResultImpl(Neo4jTemplateImpl builder) {
		this.builder = builder;
	}

	void consume(StatementResult sr) {
		try {
			if (sr.hasNext()) {
				sr.forEachRemaining(new StatementConsumer());
			}
		} finally {
			sr.consume();
		}
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

			if (builder.getAttributeMode() == CypherTemplate.AttributeMode.AUTO) {
				flatten = vals.size() == 1;
			} else if (builder.getAttributeMode() == rebar.graph.neo4j.CypherTemplate.AttributeMode.FLATTEN) {
				flatten = true;
			} else {
				flatten = false;
			}

			final boolean finalFlatten = flatten;
			vals.forEach((k, v) -> {

				if (v instanceof Entity) {
					Entity nn = Entity.class.cast(v);
					Map<String, Object> nm = nn.asMap();

					JsonNode cv = Neo4jDriverImpl.mapper.valueToTree(nm);

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
					if (v == null) {
						n.put(k, (String) null);
					}
					else if (v instanceof String) {
						n.put(k, (String) v);
					} else if (v instanceof Integer) {
						n.put(k, (Integer) v);
					} else if (v instanceof Long) {
						n.put(k, (Long) v);
					} else if (v instanceof Double) {
						n.put(k, (Double) v);
					} else if (v instanceof Float) {
						n.put(k, (Float) v);
					} else if (v instanceof BigDecimal) {
						n.put(k, (BigDecimal) v);
					} else if (v instanceof BigInteger) {
						n.put(k, (BigInteger) v);
					} else if (v instanceof Boolean) {
						n.put(k, (Boolean) v);
					} else if (v instanceof List) {
						n.set(k, Neo4jDriverImpl.mapper.valueToTree(v));
					} else if (v instanceof Map) {
						n.set(k, Neo4jDriverImpl.mapper.valueToTree(v));
					} else {
						logger.warn("unsupported type ({}): {}", k, v.getClass());
					}
				}

			});

			results.add(n);

		}
	}
}
