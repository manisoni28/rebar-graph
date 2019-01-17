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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import rebar.graph.core.RelationshipBuilder.FromNode;
import rebar.graph.neo4j.GraphDriver;
import rebar.graph.neo4j.GraphException;
import rebar.graph.neo4j.GraphSchema;

public class GraphDB {

	
	
	GraphDriver neo4j;
	Logger logger = org.slf4j.LoggerFactory.getLogger(GraphDB.class);
	static ObjectMapper mapper = new ObjectMapper();

	public long getTimestamp() {
		return neo4j.newTemplate().cypher("return timestamp() as ts").stream().findFirst().get().path("ts").asLong();
	}

	public Stream<JsonNode> matchNodesWithUpdateTsBefore(String label, long cutoff, Object... kv) {
		Map<String, Object> map = toKVMap(kv);
		String patternClause = toPatternClause(map); // need to generate BEFORE we add __graphUpdateTs
		map.put("__graphUpdateTs", cutoff);
		return neo4j.newTemplate().cypher(
				"match (a:" + label + " " + patternClause + ") where a.graphUpdateTs<{__graphUpdateTs} return a")
				.params(map).stream();
	}

	

	static String toRemoveClause(Collection<String> ra) {
		StringBuffer sb = new StringBuffer();
		if (!ra.isEmpty()) {
			sb.append(" remove ");
			AtomicInteger count = new AtomicInteger(0);
			ra.forEach(it -> {
				if (count.getAndIncrement() > 0) {
					sb.append(", ");
				}
				sb.append("a.`");
				sb.append(it);
				sb.append("`");
			});
		}
		return sb.toString();
	}




	public static final String ENTITY_TYPE="graphEntityType";
	public static final String ENTITY_GROUP="graphEntityGroup";
	public static final String UPDATE_TS="graphUpdateTs";
	
	GraphDB(GraphDriver driver) {
		this.neo4j = driver;
	}

	public GraphDriver getNeo4jDriver() {
		return this.neo4j;
	}
	/**
	 * Same as graphDB.nodes().label("mylabel")
	 * 
	 * @param label
	 * @return
	 */
	public <T extends NodeOperation> T nodes(String label) {
		return new NodeOperation().label(label);
	
	}
	static String directedMatchClause(String abbrev, String label, Map<String, Object> attrs) {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		sb.append(abbrev);
		sb.append(":");
		sb.append(label);
		sb.append(" {");

		AtomicInteger count = new AtomicInteger(0);
		attrs.keySet().forEach(k -> {
			if (count.getAndIncrement() > 0) {
				sb.append(", ");
			}
			sb.append(k);
			sb.append(":");
			sb.append("{");
			sb.append(abbrev + "_" + k);
			sb.append("}");
		});
		sb.append("}) ");
		return sb.toString();
	}
	

	public class Neo4jTagUpdate extends TagOperation {

		@Override
		public void update() {

			Preconditions.checkArgument(!Strings.isNullOrEmpty(label), "label not set");
			nodes(label).id(identifyingAttributes).merge().forEach(it -> {

				Map<String, String> desired = Maps.newHashMap();
				tags.forEach((k, v) -> {
					desired.put(prefix + k, v);
				});
				Map<String, String> existing = Maps.newHashMap();

				it.fields().forEachRemaining(f -> {
					if (f.getKey().startsWith(prefix)) {
						existing.put(f.getKey(), f.getValue().asText());
					}
				});

				MapDifference<String, String> toBeAdded = Maps.difference(desired, existing);

				if ((!toBeAdded.entriesDiffering().isEmpty()) || (!toBeAdded.entriesOnlyOnLeft().isEmpty())) {
					ObjectNode update = mapper.createObjectNode();
					identifyingAttributes.keySet().forEach(x -> {
						update.set(x, it.path(x));
					});
					toBeAdded.entriesDiffering().forEach((k, dv) -> {
						update.put(k, dv.leftValue());
					});
					toBeAdded.entriesOnlyOnLeft().forEach((k, v) -> {
						update.put(k, v);
					});

					nodes(label).properties(update).idKey(identifyingAttributes.keySet().toArray(new String[0]))
							.merge();
				}
			

			});
		}

	}
	
	public class NodeOperation {
		protected String label;
		protected Map<String, Object> idAttributes = Maps.newHashMap();

		protected Map<String, Object> dataAttributes = Maps.newHashMap();

		protected Set<String> idAttributeNames = Sets.newHashSet();

		protected Set<String> removeAttributes = Sets.newHashSet();

		protected Collection<String> shadowAttributePrefixes = ImmutableSet.of();
		
		
		protected String attributeLessThanName;
		protected long attributeLessThanValue = 0;

	
		
		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T removeProperties(String... properties) {
			if (properties != null) {
				for (String p : properties) {
					removeAttributes.add(p);
				}
			}
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T property(String n, Object val) {
			dataAttributes.put(n, val);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T properties(Map<String, Object> vals) {
			dataAttributes.putAll(vals);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T properties(JsonNode n) {

			ObjectNode val = (ObjectNode) n;
			val = stripComplexAttributes(val);
			if (n != null) {
				return (T) properties(mapper.convertValue(val, Map.class));
			}
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T label(String label) {
			this.label = label;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T id(Map<String, Object> map) {
			this.idAttributes.putAll(map);
			return (T) this;
		}

		public String getLabel() {
			return label;
		}

		public Map<String, Object> getMatchProperties() {
			return ImmutableMap.copyOf(this.idAttributes);
		}

		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T id(Object... kv) {
			idAttributes.putAll(GraphDB.toKVMap(kv));
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T id(String key, Object obj) {
			this.idAttributes.put(key, obj);
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T idKey(String... n) {
			for (String x : n) {
				this.idAttributeNames.add(x);
			}
			return (T) this;
		}

		protected void populateMatchValues() {
			idAttributeNames.forEach(it -> {
				Object val = dataAttributes.get(it);
				if (val != null) {
					idAttributes.put(it, val);
				} else {
					throw new GraphException("match key: '" + it + "' not present in data");
				}
			});
		}

	
		public Stream<JsonNode> delete() {
			String whereClause = "";
			if (!Strings.isNullOrEmpty(attributeLessThanName)) {

				if (!Strings.isNullOrEmpty(attributeLessThanName)) {
					whereClause = " where a." + attributeLessThanName + " < " + attributeLessThanValue + " ";
				}
			}
			if (this.dataAttributes != null && !dataAttributes.isEmpty()) {
				throw new GraphException("attributes cannot be set during delete");
			}
			return neo4j.newTemplate().cypher("match (a:" + label + " " + toPatternClause(idAttributes) + " ) "
					+ whereClause + " detach delete a").params(idAttributes).stream();
		}

	
		public Stream<JsonNode> match() {
			if (Strings.isNullOrEmpty(label)) {
				throw new GraphException("label not set");
			}

			populateMatchValues();

			String whereClause = "";
			if (!Strings.isNullOrEmpty(attributeLessThanName)) {
				whereClause = " where a." + attributeLessThanName + " < " + attributeLessThanValue + " ";
			}
			String setClause = dataAttributes.isEmpty() ? "" : " set a+= {__params}, a.graphUpdateTs=timestamp() ";

			String cypher = "match (a:" + label + " " + toPatternClause(idAttributes) + ") "
					+ toRemoveClause(removeAttributes) + setClause + " " + whereClause + " return a";

			Map<String, Object> combined = new HashMap<>();
			if (dataAttributes != null) {
				combined.putAll(dataAttributes);
			}
			if (idAttributes != null) {
				combined.putAll(idAttributes);
			}
			return neo4j.newTemplate().cypher(cypher).params(combined).stream();
		}

	
		public Stream<JsonNode> merge() {
			populateMatchValues();
			if (Strings.isNullOrEmpty(label)) {
				throw new GraphException("label not set");
			}
			String whereClause = "";
			if (!Strings.isNullOrEmpty(attributeLessThanName)) {
				whereClause = " where a." + attributeLessThanName + " < " + attributeLessThanValue + " ";
			}

			Map<String, Object> combined = new HashMap<>();
			if (dataAttributes != null) {
				combined.putAll(dataAttributes);
			}
			if (idAttributes != null) {
				combined.putAll(idAttributes);
			}

			String patternClause = toPatternClause(idAttributes);
			if (patternClause.trim().isEmpty()) {
				throw new GraphException("match pattern not set");
			}
			String cypher = "merge (a:" + label + " " + toPatternClause(idAttributes) + " ) "
					+ toRemoveClause(removeAttributes) + " set a+={__params}, a.graphUpdateTs=timestamp() "
					+ whereClause + " return a";

			List<JsonNode> results = neo4j.newTemplate().cypher(cypher).params(combined).stream()
					.collect(Collectors.toList());

	
			
			// look through the results and remove specific attributes that are gone.
			Set<String> attributesToBeRemoved = Sets.newHashSet();
			shadowAttributePrefixes.forEach(prefix -> {
				results.forEach(it -> {
					it.fieldNames().forEachRemaining(attr -> {
						if (attr.startsWith(prefix) && (!combined.containsKey(attr))) {
							attributesToBeRemoved.add(attr);
						}
					});
				});
			});

			if (!attributesToBeRemoved.isEmpty())  {
				logger.info("removing shadow attributes: {}",attributesToBeRemoved);
				cypher = "merge (a:" + label + " " + toPatternClause(idAttributes) + " ) "
						+ toRemoveClause(attributesToBeRemoved);
				
				neo4j.newTemplate().cypher(cypher).params(combined).exec();
			}
			
			return results.stream();
		}

		@SuppressWarnings("unchecked")
		public RelationshipBuilder.Relationship relationship(String name) {
			FromNode n = new RelationshipBuilder().driver(getNeo4jDriver()).from(this.label);
			
			for (String it: idAttributeNames) {
				n = n.id(it, dataAttributes.get(it));
			}
			for (Entry<String,Object> e: idAttributes.entrySet()) {
				n = n.id(e.getKey(),e.getValue());
			}
			return n.relationship(name);
			
		}

		/**
		 * This is a rebar-graph specific condition for graphUpdateTs. No real interest in
		 * abstracting ad-hoc query predicates. We are not trying to create a general
		 * purpose inteface for working with the underlying graph...just make it easy to
		 * build graphs.
		 * 
		 * @param name
		 * @param val
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public <T extends NodeOperation> T whereAttributeLessThan(String name, long val) {
			this.attributeLessThanName = name;
			this.attributeLessThanValue = val;
			return (T) this;
		}

		public  <T extends NodeOperation> T withTagPrefixes(Set<String> prefixes) {
			if (prefixes==null) {
				this.shadowAttributePrefixes = ImmutableSet.of();
			}
			else {
				this.shadowAttributePrefixes = ImmutableSet.copyOf(prefixes);
			}
			return (T) this;
		}
	}


	/*
	 * public abstract class MatchNodesOperation extends NodeOperation { protected
	 * String attribbuteLessThanName = ""; protected long attributeLessThan = 0;
	 * 
	 * public <T extends MatchNodesOperation> T whereAttributeLessThan(String name,
	 * long val) { this.attribbuteLessThanName = name; this.attributeLessThan = val;
	 * return (T) this; } }
	 */

	public abstract class TagOperation {

		protected String label;
		protected Map<String, Object> identifyingAttributes;
		protected String prefix = "tag_";
		protected Map<String, String> tags = Maps.newHashMap();

		public TagOperation withPrefix(String prefix) {
			this.prefix = prefix;
			return this;
		}

		public TagOperation withTag(String key, String val) {
			this.tags.put(key, val);
			return this;
		}

		public TagOperation withTags(Map<String, String> tags) {
			this.tags.putAll(tags);
			return this;
		}

		public abstract void update();

	}

	protected static boolean isSingleTypeArray(JsonNode n) {
		if (!n.isArray()) {
			return false;
		}
		ArrayNode an = (ArrayNode) n;

		JsonNodeType type = null;
		for (int i = 0; i < an.size(); i++) {
			JsonNode x = an.get(i);
			if (x == null) {
				// cannot store null values in arrays
				return false;
			}
			if (x.isContainerNode()) {
				return false;
			}
			if (type != null && x.getNodeType() != type) {
				return false;
			}
			if (type == null) {
				type = x.getNodeType();
			}

		}

		return true;
	}

	protected ObjectNode stripComplexAttributes(ObjectNode n) {
		List<String> attributesToStrip = Lists.newArrayList();

		n.fields().forEachRemaining(it -> {
			if (it.getValue() != null && it.getValue().isContainerNode()) {
				if (isSingleTypeArray(it.getValue())) {
					// ok
				} else {
					attributesToStrip.add(it.getKey());
				}
			}
		});

		if (attributesToStrip.isEmpty()) {
			return n;
		}
		logger.info("not persisting complex attributes for {}: {}", n.path(ENTITY_TYPE).asText(), attributesToStrip);
		ObjectNode copy = n.deepCopy();
		copy.remove(attributesToStrip);
		return copy;
	}

	public GraphSchema schema() {
		return this.neo4j.schema();
	}
	


	static String toPatternClause(Map<String, Object> m) {
		if (m == null || m.isEmpty()) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		AtomicInteger count = new AtomicInteger(0);
		m.keySet().forEach(it -> {
			if (count.getAndIncrement() != 0) {
				sb.append(", ");
			}
			sb.append(it);
			sb.append(":{");
			sb.append(it);
			sb.append("}");
		});
		sb.append("}");
		return sb.toString();
	}

	public static Map<String, Object> toKVMap(Object... kv) {
		Map<String, Object> m = Maps.newHashMap();
		if (kv != null) {
			if (kv.length % 2 != 0) {
				throw new IllegalArgumentException("list of key-value pairs must be an even length");
			}
			for (int i = 0; i < kv.length; i += 2) {
				m.put(kv[i].toString(), kv[i + 1]);
			}
		}
		return m;
	}


}
