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

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import rebar.graph.driver.GraphDriver;
import rebar.graph.driver.GraphException;
import rebar.graph.neo4j.Neo4jDriver;

public class RebarGraph {

	static org.slf4j.Logger logger = LoggerFactory.getLogger(RebarGraph.class);
	

	Map<String, Supplier<? extends Scanner>> supplierMap = Maps.newConcurrentMap();

	GraphDB graphWriter;
	
	private RebarGraph() {

	}

	public static class Builder {

		GraphDB graphDb;
		
		Map<String,String> env = Maps.newHashMap();

		Graph g;

		public Builder withGraphDB(GraphDB graphWriter) {
			this.graphDb = graphWriter;
			return this;
		}
		

		public Builder withGraphPassword(String password) {
			env.put("GRAPH_PASSWORD", password);
			return this;
		}
		public Builder withGraphUsername(String username) {
			env.put("GRAPH_USERNAME", username);
			return this;
		}
		public Builder withGraphUrl(String url) {
			env.put("GRAPH_URL", url);
			return (this);
		}

		public Builder withInMemoryTinkerGraph() {
			env.put("GRAPH_URL", "memory");
			return this;
		}

		public Optional<String> getEnv(String name) {
			String val = env.get(name);
			if (val==null) {
				val = System.getenv(name);
			}
			
			return Optional.ofNullable(val);
		}
	
		public RebarGraph build() {

			RebarGraph rg = new RebarGraph();

			if (graphDb!=null) {
				rg.graphWriter = graphDb;
				return rg;
			}
			
			Optional<String> graphUrl = getEnv("GRAPH_URL");
			if (graphUrl.isPresent()) {
				
				GraphDriver.Builder b = new GraphDriver.Builder().withUrl(graphUrl.get());
				if (env.containsKey("GRAPH_USERNAME") && env.containsKey("GRAPH_PASSWORD")) {
					b =b.withUsername(env.get("GRAPH_USERNAME")).withPassword(env.get("GRAPH_PASSWORD"));
				}
				GraphDriver driver = b.build();
				if (driver.getClass().getName().toLowerCase().contains("neo4j")) {
					Neo4jGraphDB gw = new Neo4jGraphDB((Neo4jDriver) driver);
					rg.graphWriter = gw;
					return rg;
				}
				else {
					throw new GraphException("GRAPH_URL "+graphUrl.get()+" not supported");
				}
				
			}
			else {
				throw new GraphException("GRAPH_URL not set");
			}
			
		

		}
	}

	public <T extends ScannerBuilder<? extends Scanner>> T createBuilder(Class<T> clazz) {
		try {
			T t = (T) clazz.newInstance();
			t.setRebarGraph(this);
			Preconditions.checkNotNull(t.getRebarGraph());
			return t;
		} catch (IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}

	public GraphDB getGraphDB() {
		return graphWriter;
	}
	




	public <T extends Scanner> void registerScanner(Class<T> scannerType, String name, Supplier<T> supplier) {
		String key = scannerType.getName() + ":" + name;
		if (supplierMap.containsKey(key)) {
			throw new IllegalStateException("already exists: " + key);
		}
		supplierMap.put(key, supplier);
	}

	@SuppressWarnings("unchecked")
	public <T extends Scanner> T getScanner(Class<T> scannerType, String name) {
		Supplier<? extends Scanner> supplier = supplierMap.get(scannerType.getName() + ":" + name);
		if (supplier == null) {
			throw new RuntimeException("not found: " + name);
		}
		return (T) supplier.get();
	}
}
