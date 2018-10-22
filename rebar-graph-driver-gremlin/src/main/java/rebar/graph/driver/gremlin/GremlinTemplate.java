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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import rebar.graph.driver.GraphTemplate;

public class GremlinTemplate extends GraphTemplate {

	static ObjectMapper mapper = new ObjectMapper();

	GremlinDriver driver;
	Function<GraphTraversalSource, GraphTraversal> gremlin;



	protected GremlinTemplate copy() {
		GremlinTemplate copy = new GremlinTemplate(driver);
		copy.gremlin = gremlin;
		copy.attributeMode = attributeMode;
		copy.resultLimit = resultLimit;
		return copy;
	}
	class JsonifyFunction implements Function<Object, JsonNode> {

		@SuppressWarnings("unchecked")
		@Override
		public JsonNode apply(Object c) {

		
			boolean flatten = true;
			if (attributeMode == AttributeMode.FLATTEN) {
				flatten = true;
			} else if (attributeMode == AttributeMode.HIERARCHICAL) {
				flatten = false;
			} else {
				if (c instanceof Map && ((Map) c).size() == 1) {
					flatten = true;
				} else {
					flatten = false;
				}
			}
			boolean finalFlatten = flatten;
			final ObjectNode n = mapper.createObjectNode();
			if (c instanceof Vertex) {
				toJsonNode((Vertex) c, n, null);
				return n;
			} else if (c instanceof Map) {

				Map<Object, Object> m = (Map<Object, Object>) c;
				m.forEach((k, v) -> {
					if (v instanceof Vertex) {
						if (finalFlatten) {
							ObjectNode nn = toJsonNode((Vertex) v, mapper.createObjectNode(), k.toString());
							n.setAll(nn);
						} else {
							ObjectNode nn = toJsonNode((Vertex) v, mapper.createObjectNode(), null);
							n.set(k.toString(), nn);
						}
					} else if (v instanceof Edge) {
						if (finalFlatten) {
							ObjectNode nn = toJsonNode((Edge) v, mapper.createObjectNode(), k.toString());
							n.setAll(nn);
						} else {
							ObjectNode nn = toJsonNode((Edge) v, mapper.createObjectNode(), null);
							n.set(k.toString(), nn);
						}
					} else if (v instanceof List) {
						ArrayNode an = (ArrayNode) mapper.valueToTree(v);
						if (an.size() == 0) {
							n.put(k.toString(), (String) null);
						} else if (an.size() == 1) {
							n.set(k.toString(), an.get(0));
						} else {
							n.set(k.toString(), an);
						}
					} else if (v instanceof Map) {
						ObjectNode xx = mapper.valueToTree(v);

						ObjectNode copy = mapper.createObjectNode();
						xx.fields().forEachRemaining(it -> {
							if (it.getValue().isArray()) {
								ArrayNode an = (ArrayNode) it.getValue();
								if (an.size() == 0) {
									copy.put(it.getKey(), (String) null);
								} else if (an.size() == 1) {
									copy.set(it.getKey(), an.get(0));
								} else {
									copy.set(it.getKey(), an);
								}
							} else {
								copy.set(it.getKey(), it.getValue());
							}
						});
						if (finalFlatten) {
							copy.fields().forEachRemaining(it->{
								n.set(k.toString()+"."+it.getKey(), it.getValue());
							});
						} else {
							n.set(k.toString(), copy);
						}

					} else {
						JsonNode xx = mapper.valueToTree(v);

						n.set(k.toString(), xx);

					}
				});

			} 
			else if (c instanceof Edge) {
				toJsonNode((Edge) c, n, null);
				return n;
			}
			else if (c instanceof String || c instanceof Number || c instanceof Boolean) {
				return mapper.valueToTree(c);
				
			}
			else {
				throw new RuntimeException("unsupported type " + c.getClass());
			}
			return n;
		}

	}

	GremlinTemplate(GremlinDriver driver) {
		this.driver = driver;
	}

	GremlinTemplate gremlin(Function<GraphTraversalSource, GraphTraversal> f) {
		this.gremlin = f;
		return this;
	}

	

	private class MaxRowsPredicate implements Predicate<Object> {
		AtomicInteger count = new AtomicInteger(0);

		@Override
		public boolean test(Object t) {
			return resultLimit < 1 || count.getAndIncrement() < resultLimit;
		}
	}

	public void delete() {
		gremlin.apply(driver.traversal()).drop().iterate();
	}

	public void exec() {
		gremlin.apply(driver.traversal()).iterate();
	}

	@SuppressWarnings({ "unchecked" })
	public Stream<JsonNode> stream() {

		return gremlin.apply(driver.traversal()).toStream().filter(new MaxRowsPredicate()).map(new JsonifyFunction());

	}

	public Optional<JsonNode> findFirst() {
		return stream().findFirst();
	}
	public List<JsonNode> list() {
		return stream().collect(Collectors.toList());
	}
	public void forEach(Consumer<JsonNode> x) {
		stream().forEach(x);
	}

	@SuppressWarnings("unchecked")
	Map<String, Object> toParams(JsonNode n) {
		if (n == null) {
			return new HashMap<>();
		}
		return mapper.convertValue(n, Map.class);
	}

	ObjectNode toJsonNode(Element e, ObjectNode n, String prefix) {

		boolean elementMetadata=true;
		if (elementMetadata) {
			Object id = e.id();
			String idKey = prefix == null ? "id" : prefix + ".id";
			String labelKey = prefix == null ? "label" : prefix + ".label";
			if (id instanceof Integer) {
				n.put(idKey, (Integer) id);
			} else if (id instanceof Long) {
				n.put(idKey, (Long) id);
			} else if (id instanceof String) {
				n.put(idKey, (String) id);
			}

			n.put(labelKey, e.label());
		}

		for (String k : e.keys()) {
			String key = k;

			Iterator<? extends Property<Object>> t = e.properties(key);
			while (t.hasNext()) {
				Property<Object> vp = (Property<Object>) t.next();
				if (prefix != null) {
					key = prefix + "." + k;
				}
				setProperty(n, key, vp);
			}
		}
		return n;
	}

	private void setProperty(ObjectNode n, String key, Property<?> vp) {
		if (vp != null && vp.isPresent()) {
			Object val = vp.value();
			if (val instanceof String) {
				n.put(key, val.toString());
			} else if (val instanceof Integer) {
				n.put(key, (Integer) val);
			} else if (val instanceof Long) {
				n.put(key, (Long) val);
			} else if (val instanceof Boolean) {
				n.put(key, (Boolean) val);
			} else if (val instanceof Float) {
				n.put(key, (Float) val);
			} else if (val instanceof Double) {
				n.put(key, (Double) val);
			} else if (val instanceof BigDecimal) {
				n.put(key, (BigDecimal) val);
			} else if (val instanceof BigInteger) {
				n.put(key, (BigInteger) val);
			} else if (val instanceof List) {
				n.set(key, mapper.valueToTree(val));
			} else if (val instanceof Map) {
				n.set(key, mapper.valueToTree(val));
			} else {
				throw new RuntimeException("unsupported type: " + val.getClass() + " (" + val + ")");
			}
		} else {
			n.set(key, null);
		}
	}



}
