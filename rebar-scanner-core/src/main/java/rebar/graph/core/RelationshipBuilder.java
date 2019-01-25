package rebar.graph.core;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import rebar.graph.neo4j.CypherTemplate;
import rebar.graph.neo4j.CypherUtil;
import rebar.graph.neo4j.GraphDriver;

public class RelationshipBuilder {

	private GraphDriver driver;

	private String aLabel;

	private String bLabel;

	private String relationshipName;

	private Map<String, Object> sourceIdAttributes = Maps.newHashMap();
	private Map<String, Object> targetIdAttributes = Maps.newHashMap();

	private List<JoinOn> joinOn = Lists.newArrayList();


	Logger logger = LoggerFactory.getLogger(RelationshipBuilder.class);
	public static enum Cardinality {
		ONE,
		MANY
	}
	public class FromNode {
		public FromNode label(String name) {
			sourceNodeType(name);
			return this;
		}

		public FromNode id(String key, Object val) {
			sourceIdAttribute(key, val);
			return this;
		}

		public Relationship relationship(String name) {
			RelationshipBuilder.this.relationship(name);
			return new Relationship();
		}

	}
	static class JoinOn {
		Cardinality toCardinality;
		String fromAttribute;
		String toAttribute;
	}
	public class Relationship {

		public Relationship on(String fromAttribute, Cardinality cardinality, String toAttribute) {
			return this;
		}
		public Relationship on(String fromAttribue, String toAttribute) {
			on(fromAttribue,toAttribute,Cardinality.ONE);
			return this;
		}
		public Relationship on(String fromAttribute, String toAttribute, Cardinality cardinality) {
			JoinOn j = new JoinOn();
			j.fromAttribute = fromAttribute;
			j.toAttribute = toAttribute;
			j.toCardinality = cardinality;
			joinOn.add(j);
			
			return this;
		}

		public ToNode to(String name) {
			targetNodeType(name);
			return new ToNode();
		}

	}

	public class ToNode {

		public ToNode id(String key, Object val) {
			targetIdAttribute(key, val);
			return this;
		}

		public void merge() {
			RelationshipBuilder.this.merge();
		}
	}

	public <T extends RelationshipBuilder> T sourceIdAttribute(String key, Object val) {
		sourceIdAttributes.put(key, val);
		return (T) this;
	}

	public <T extends RelationshipBuilder> T targetIdAttribute(String key, Object val) {
		targetIdAttributes.put(key, val);
		return (T) this;
	}

	public <T extends RelationshipBuilder> T driver(GraphDriver d) {
		this.driver = d;
		return (T) this;
	}

	public FromNode from(String name) {
		sourceNodeType(name);
		return new FromNode();
	}

	private <T extends RelationshipBuilder> T sourceNodeType(String name) {
		this.aLabel = name;
		return (T) this;
	}

	private <T extends RelationshipBuilder> T targetNodeType(String name) {
		this.bLabel = name;
		return (T) this;
	}

	private <T extends RelationshipBuilder> T relationship(String name) {
		this.relationshipName = name;
		return (T) this;
	}

	public void merge() {
		mergeRelationships();
		deleteStaleRelationships();
	}

	protected String joinClause() {
		StringBuffer sb = new StringBuffer();
		int c = 0;
		if (joinOn.isEmpty()) {
			return sb.toString();
		} else {
			for (JoinOn on : joinOn) {
				String cardinalityComparator = on.toCardinality==Cardinality.ONE ? "="  : "in";
				sb.append(String.format(" %s b.%s %s a.%s ", (c++ > 0)? " AND " : "", CypherUtil.escapePropertyName(on.toAttribute), cardinalityComparator,CypherUtil.escapePropertyName(on.fromAttribute)));
			}
		}

		return sb.toString();
	}

	private void mergeRelationships() {

		Preconditions.checkArgument(!Strings.isNullOrEmpty(aLabel), "source node label not set");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(bLabel), "target node label not set");

		String cypher = String.format("match (a:%s %s), (b:%s %s)", aLabel, toMatchPattern(sourceIdAttributes, "a_"),
				bLabel,

				toMatchPattern(targetIdAttributes, "b_"));

		String whereClause = joinClause();

		if (!whereClause.trim().isEmpty()) {
			whereClause = " where " + whereClause;
		}
		cypher = cypher
				+ String.format(" %s merge (a)-[r:%s]->(b) return count(r) as count", whereClause, relationshipName);

		logger.debug("create relationships: {}", cypher);
		
		CypherTemplate template = driver.cypher(cypher);
		for (Entry<String, Object> entry : sourceIdAttributes.entrySet()) {
			template = template.param("a_" + entry.getKey(), entry.getValue());

		}

		for (Entry<String, Object> entry : targetIdAttributes.entrySet()) {
			template = template.param("b_" + entry.getKey(), entry.getValue());

		}

		long count = template.findFirst().get().path("count").asLong();

		logger.debug("relationship count: {}", count);

	}

	

	private void deleteStaleRelationships() {

		if (joinOn.isEmpty()) {
			return;
		}
		String cypher = String.format("match (a:%s %s)-[r:%s]->(b:%s %s)", aLabel,
				toMatchPattern(sourceIdAttributes, "a_"), relationshipName, bLabel,
				toMatchPattern(targetIdAttributes, "b_"));

		cypher = cypher + String.format(" where NOT (%s) delete r return count(r) as count",joinClause());

		logger.debug("delete stale relationships: {}", cypher);
		CypherTemplate template = driver.cypher(cypher);

		for (Entry<String, Object> entry : sourceIdAttributes.entrySet()) {
			template = template.param("a_" + entry.getKey(), entry.getValue());
		}

		for (Entry<String, Object> entry : targetIdAttributes.entrySet()) {
			template = template.param("b_" + entry.getKey(), entry.getValue());
		}
		long count = template.findFirst().get().path("count").asLong();
		if (count > 0) {
			logger.info("deleted relationships: {}", count);
		} else {
			logger.debug("deleted relationships: {}", count);
		}
	}

	protected String toMatchPattern(Map<String, Object> attrs, String prefix) {
		if (attrs == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		AtomicInteger count = new AtomicInteger(0);
		sb.append("{");
		attrs.forEach((a, b) -> {
			if (count.getAndIncrement() > 0) {
				sb.append(",");
			}
			sb.append(" ");

			sb.append(a);
			sb.append(":{");

			sb.append(prefix);
			sb.append(a);
			sb.append("} ");
		});
		sb.append("}");
		return sb.toString();
	}
}
