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

import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import rebar.graph.driver.GraphException;
import rebar.graph.driver.GraphSchema;
import rebar.graph.driver.gremlin.GremlinDriver;

public class GremlinDB extends GraphDB {

	GremlinDriver driver;
	
	class GremlinNodeOperation extends NodeOperation {

		@Override
		public <T extends RelationshipOperation> T relationship(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Stream<JsonNode> merge() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Stream<JsonNode> delete() {
			if (!Strings.isNullOrEmpty(attributeLessThanName)) {
				throw new GraphException("less than predicate not supported for delete");
			}
			if (this.dataAttributes!=null && !dataAttributes.isEmpty()) {
				throw new GraphException("attributes cannot be set during delete");
			}
			return driver.gremlin(g->{ 
				GraphTraversal<Vertex, Vertex> gt = g.V().hasLabel(label);
				
				for (Entry<String,Object> it: idAttributes.entrySet()) {
					gt = gt.has(it.getKey(),it.getValue());
				}
				
				return gt;
			
			}).stream();
		
		}

		@Override
		public Stream<JsonNode> match() {
			throw new UnsupportedOperationException();
		}
		
	}
	GremlinDB(GremlinDriver driver) {
		this.driver = driver;
	}

	@Override
	public long getTimestamp() {
		return System.currentTimeMillis();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeOperation> T nodes() {
		return (T) new GremlinNodeOperation();
	}

	public GraphSchema schema() {
		return driver.schema();
	}
}
