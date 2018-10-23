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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import rebar.graph.driver.GraphException;
import rebar.graph.driver.GraphSchema;
import rebar.graph.neo4j.Neo4jDriver;

public class Neo4jGraphDB extends GraphDB {

	static Logger logger = LoggerFactory.getLogger(Neo4jGraphDB.class);
	ObjectMapper mapper = new ObjectMapper();
	Neo4jDriver neo4j;

	public class Neo4jTagUpdate extends TagOperation {

		@Override
		public void update() {

			Preconditions.checkArgument(!Strings.isNullOrEmpty(label), "label not set");
			nodes().label(label).id(identifyingAttributes).merge().forEach(it -> {

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

					nodes().label(label).properties(update)
							.idKey(identifyingAttributes.keySet().toArray(new String[0])).merge();
				}
				if (!toBeAdded.entriesOnlyOnRight().isEmpty()) {

					// removeAttributes().label(label),identifyingAttributes).attributes(toBeAdded.entriesOnlyOnRight().keySet()).update();

				}

			});
		}

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

	Neo4jGraphDB(Neo4jDriver driver) {
		this.neo4j = driver;
	}

	public Neo4jDriver getNeo4jDriver() {
		return this.neo4j;
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

	public long getTimestamp() {
		return neo4j.newTemplate().cypher("return timestamp() as ts").stream().findFirst().get().path("ts").asLong();
	}

	public Stream<JsonNode> matchNodesWithUpdateTsBefore(String label, long cutoff, Object... kv) {
		Map<String, Object> map = toKVMap(kv);
		String patternClause = toPatternClause(map); // need to generate BEFORE we add __graphUpdateTs
		map.put("__graphUpdateTs", cutoff);
		return neo4j.newTemplate()
				.cypher("match (a:" + label + " " + patternClause + ") where a.graphUpdateTs<{__graphUpdateTs} return a")
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

	public class Neo4jNodeOperation extends NodeOperation {

		@Override
		public Stream<JsonNode> delete() {
			String whereClause = "";
			if (!Strings.isNullOrEmpty(attributeLessThanName)) {
				
				if (!Strings.isNullOrEmpty(attributeLessThanName)) {
					whereClause = " where a." + attributeLessThanName + " < " + attributeLessThanValue + " ";
				}
			}
			if (this.dataAttributes!=null && !dataAttributes.isEmpty()) {
				throw new GraphException("attributes cannot be set during delete");
			}
			return neo4j.newTemplate()
					.cypher("match (a:" + label + " " + toPatternClause(idAttributes) + " ) "+whereClause+" detach delete a").params(idAttributes)
					.stream();
		}

		@Override
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
					+ toRemoveClause(removeAttributes) + setClause+" " + whereClause + " return a";

			
			Map<String,Object> combined  = new HashMap<>();
			if (dataAttributes!=null) {
				combined.putAll(dataAttributes);
			}
			if (idAttributes!=null) {
				combined.putAll(idAttributes);
			}
			return neo4j.newTemplate().cypher(cypher).params(combined).stream();
		}

		@Override
		public Stream<JsonNode> merge() {
			populateMatchValues();
			if (Strings.isNullOrEmpty(label)) {
				throw new GraphException("label not set");
			}
			String whereClause = "";
			if (!Strings.isNullOrEmpty(attributeLessThanName)) {
				whereClause = " where a." + attributeLessThanName + " < " + attributeLessThanValue + " ";
			}
			
			Map<String,Object> combined  = new HashMap<>();
			if (dataAttributes!=null) {
				combined.putAll(dataAttributes);
			}
			if (idAttributes!=null) {
				combined.putAll(idAttributes);
			}
			
			String patternClause = toPatternClause(idAttributes);
			if (patternClause.trim().isEmpty()) {
				throw new GraphException("match pattern not set");
			}
			String cypher = "merge (a:" + label + " " + toPatternClause(idAttributes) + " ) "
					+ toRemoveClause(removeAttributes) + " set a+={__params}, a.graphUpdateTs=timestamp() "+whereClause+" return a";
		
			return neo4j.newTemplate().cypher(cypher).params(combined).stream();

		}
		@SuppressWarnings("unchecked")
		public <T extends RelationshipOperation> T relationship(String name) {
			Neo4jRelationshipOperation ro = new Neo4jRelationshipOperation(this,name);
			
			
			return (T) ro;
		}
	}

	public class Neo4jRelationshipTargetNodeOperation extends RelationshipTargetNodeOperation {

		Neo4jRelationshipOperation relOp;
		
		public Neo4jRelationshipTargetNodeOperation(Neo4jRelationshipOperation relOp) {
			this.relOp = relOp;
		}
		
		String fromClause() {
			return directedMatchClause("from", relOp.from.getLabel(), relOp.from.getMatchProperties());
		}

		String toClause() {
			return directedMatchClause("to", getLabel(), getMatchProperties());
		}
		
		String whereClause() {
			StringBuffer sb = new StringBuffer();
			if (!relOp.joinAttributes.isEmpty()) {
				sb.append(" where ");
			}
			AtomicInteger count = new AtomicInteger(0);
			relOp.joinAttributes.forEach((a,b)->{
				if (count.getAndIncrement()>0) {
					sb.append(" and ");
				}
				sb.append(" from.`");
				sb.append(a);
				sb.append("` = to.`");
				sb.append(b);
				sb.append("` ");
			});
			
			
			return sb.toString();
		}
		public Stream<JsonNode> merge() {

			String cypher = "match " + fromClause() + "," + toClause() + whereClause() +" merge (from)-[r:" + this.relOp.relationshipType
					+ "]->(to) set r.graphUpdateTs=timestamp() return r";

			
			Map<String, Object> combined = Maps.newHashMap();

			relOp.from.getMatchProperties().forEach((k, v) -> {
				combined.put("from_" + k, v);
			});
			getMatchProperties().forEach((k, v) -> {
				combined.put("to_" + k, v);
			});

			return getNeo4jDriver().newTemplate().cypher(cypher).params(combined).stream();

		}
		
		@Override
		public <T extends RelationshipOperation> T relationship(String name) {
			throw new UnsupportedOperationException();
		}


		@Override
		public Stream<JsonNode> delete() {
			String cypher = "match " + fromClause() + "-[r:"+this.relOp.relationshipType+"]->"+ toClause() + "  detach delete r return r";

			Map<String, Object> combined = Maps.newHashMap();

			relOp.from.getMatchProperties().forEach((k, v) -> {
				combined.put("from_" + k, v);
			});
			getMatchProperties().forEach((k, v) -> {
				combined.put("to_" + k, v);
			});
			if (!relOp.joinAttributes.isEmpty()) {
				throw new GraphException("join attributes not supported for deleting relationships");
			}
			return getNeo4jDriver().newTemplate().cypher(cypher).params(combined).stream();
		}

		@Override
		public Stream<JsonNode> match() {
			throw new UnsupportedOperationException();
		}
		
	}
	public class Neo4jRelationshipOperation extends RelationshipOperation {

		NodeOperation from;
		String relationshipType;
		Neo4jRelationshipOperation(NodeOperation from, String relationshipType) {
			this.from = from;
			this.relationshipType = relationshipType;
		}
		@Override
		public RelationshipTargetNodeOperation to(String label) {
			RelationshipTargetNodeOperation target = new Neo4jRelationshipTargetNodeOperation(this).label(label);
		
			return target;
		}
		
	}
	@Override
	@SuppressWarnings("unchecked")
	public <T extends NodeOperation> T nodes() {

		return (T) new Neo4jNodeOperation();
	}
	
	public GraphSchema schema() {
		return this.neo4j.schema();
	}
	
}
