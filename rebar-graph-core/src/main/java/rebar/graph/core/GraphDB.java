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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import rebar.graph.driver.GraphException;
import rebar.graph.driver.GraphSchema;

public abstract class GraphDB {

	Logger logger = org.slf4j.LoggerFactory.getLogger(GraphDB.class);
	static ObjectMapper mapper = new ObjectMapper();

	public abstract long getTimestamp();

	public abstract <T extends NodeOperation> T nodes();

	public static final String ENTITY_TYPE="graphEntityType";
	public static final String ENTITY_GROUP="graphEntityGroup";
	public static final String UPDATE_TS="graphUpdateTs";
	
	/**
	 * Same as graphDB.nodes().label("mylabel")
	 * 
	 * @param label
	 * @return
	 */
	public <T extends NodeOperation> T nodes(String label) {
		return nodes().label(label);
	}

	public abstract class RelationshipTargetNodeOperation extends NodeOperation {

	}

	public abstract class RelationshipOperation {

		protected Map<String, String> joinAttributes = Maps.newHashMap();

		public abstract RelationshipTargetNodeOperation to(String label);

		@SuppressWarnings("unchecked")
		public <T extends RelationshipOperation> T on(String fromProperty, String toProperty) {

			Preconditions.checkArgument(!Strings.isNullOrEmpty(fromProperty),
					"LHS of join property cannot be null or empty");
			Preconditions.checkArgument(!Strings.isNullOrEmpty(toProperty),
					"RHS of join property cannot be null or empty");
			joinAttributes.put(fromProperty, toProperty);
			return (T) this;
		}

	}

	public abstract class NodeOperation {
		protected String label;
		protected Map<String, Object> idAttributes = Maps.newHashMap();

		protected Map<String, Object> dataAttributes = Maps.newHashMap();

		protected Set<String> idAttributeNames = Sets.newHashSet();

		protected Set<String> removeAttributes = Sets.newHashSet();

		protected Collection<String> shadowAttributePrefixes = ImmutableSet.of();
		
		protected RelationshipOperation relOp;
		protected String attributeLessThanName;
		protected long attributeLessThanValue = 0;

		public abstract <T extends RelationshipOperation> T relationship(String name);

		
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
			idAttributes.putAll(Neo4jGraphDB.toKVMap(kv));
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

		public abstract Stream<JsonNode> merge();

		public abstract Stream<JsonNode> delete();

		public abstract Stream<JsonNode> match();

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

	public abstract GraphSchema schema();
	
	
	

}
