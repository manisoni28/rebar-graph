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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.neo4j.CypherTemplate.AttributeMode;

public abstract class CypherTemplate {

	public static enum AttributeMode {
		AUTO, FLATTEN, HIERARCHICAL
	}
	
	protected int resultLimit = -1;
	protected AttributeMode attributeMode = AttributeMode.AUTO;
	
	public long getMaxResults() {
		return resultLimit;
	}
	public AttributeMode getAttributeMode() {
		return attributeMode;
	}

	protected abstract <T extends CypherTemplate> T copy();
	
	@SuppressWarnings("unchecked")
	public<T extends CypherTemplate> T withMaxResults(int i) {
		this.resultLimit = i;
		return (T) this;
	}
	public abstract CypherTemplate cypher(String cypher);
	public abstract Stream<JsonNode> stream();
	public abstract List<JsonNode> list();
	public abstract Optional<JsonNode> findFirst();
	public abstract void exec();
	public abstract void forEach(Consumer<JsonNode> c);
	public abstract CypherTemplate withAttributeMode(AttributeMode m);
	public abstract CypherTemplate param(String name, Object val);
	public abstract CypherTemplate params(Map<?,?> map);
	public abstract CypherTemplate params(Object...args);
}
