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

import static rebar.graph.neo4j.GraphDriver.GRAPH_PASSWORD;
import static rebar.graph.neo4j.GraphDriver.GRAPH_URL;
import static rebar.graph.neo4j.GraphDriver.GRAPH_USERNAME;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import rebar.graph.neo4j.GraphDriver;
import rebar.graph.neo4j.GraphException;
import rebar.util.EnvConfig;
import rebar.util.RebarException;

public class RebarGraph {

	static org.slf4j.Logger logger = LoggerFactory.getLogger(RebarGraph.class);

	Map<String, Supplier<? extends Scanner>> supplierMap = Maps.newConcurrentMap();

	GraphDB graphWriter;

	EnvConfig env = null;

	ScanQueue queue;

	String scannerId = UUID.randomUUID().toString();
	
	private RebarGraph() {

	}

	public static class Builder {

		GraphDB graphDb;

		EnvConfig env = new EnvConfig();

	

		String scannerId;
		
		public Builder withEnv(EnvConfig cfg) {
			this.env = cfg.copy();
			return this;
		}

		public Builder withGraphDB(GraphDB graphWriter) {
			this.graphDb = graphWriter;
			return this;
		}

		public Builder withGraphPassword(String password) {
			env = env.withEnv(GRAPH_PASSWORD, password);
			return this;
		}

		public Builder withGraphUsername(String username) {
			env = env.withEnv(GRAPH_USERNAME, username);
			return this;
		}

		public Builder withGraphUrl(String url) {
			env = env.withEnv(GRAPH_URL, url);
			return (this);
		}

		public Builder withScannerId(String id) {
			this.scannerId=id;
			return this;
		}
		public Builder withInMemoryTinkerGraph() {
			env = env.withEnv(GRAPH_URL, "memory");
			return this;
		}

		public Optional<String> getEnv(String name) {
			return env.get(name);
		}

		public RebarGraph build() {

			RebarGraph rg = new RebarGraph();

			if (graphDb != null) {
				rg.graphWriter = graphDb;
				rg.env = env;

				
					Neo4jScanQueue queue = new Neo4jScanQueue(graphDb.getNeo4jDriver());
					queue.start();
					rg.queue = queue;
				
				return rg;
			}

			Optional<String> graphUrl = getEnv(GRAPH_URL);
			if (!graphUrl.isPresent()) {

				graphUrl = Optional.of("bolt://localhost:7687");

				logger.info("GRAPH_URL not set ... defaulting to {}", graphUrl.get());
			}
			logger.info("GRAPH_URL: {}", graphUrl.orElse(""));
			if (graphUrl.isPresent()) {

				GraphDriver.Builder b = new GraphDriver.Builder().withUrl(graphUrl.get());
				if (env.get(GRAPH_USERNAME).isPresent() && env.get(GRAPH_PASSWORD).isPresent()) {
					b = b.withUsername(env.get(GRAPH_USERNAME).get()).withPassword(env.get(GRAPH_PASSWORD).get());
				}
				GraphDriver driver = b.build();
				if (driver.getClass().getName().toLowerCase().contains("neo4j")) {
					GraphDB gw = new GraphDB((GraphDriver) driver);
					rg.graphWriter = gw;
					rg.env = env;
					Neo4jScanQueue queue = new Neo4jScanQueue((GraphDriver) driver);
					queue.start();
					rg.queue = queue;
					return rg;
				} else {
					throw new GraphException("GRAPH_URL " + graphUrl.get() + " not supported");
				}

			} else {
				throw new GraphException("GRAPH_URL not set");
			}

		}
	}

	protected <T extends ScannerBuilder<? extends Scanner>> T createBuilder() {
		try {
			Optional<String> scannerClass = env.get("REBAR_SCANNER");
			if (!scannerClass.isPresent()) {
				throw new RebarException("REBAR_SCANNER not set");
			}
			String className = scannerClass.get();
			if (!className.endsWith("Builder")) {
				className = className + "Builder";
			}
			Class clazz = Class.forName(className);

			T t = (T) createBuilder(clazz);

			return t;
		} catch (ClassNotFoundException e) {
			throw new RebarException(e);
		}

	}

	public <T extends ScannerBuilder<? extends Scanner>> T createBuilder(Class<T> clazz) {
		try {
			T t = (T) clazz.newInstance();
			t.setRebarGraph(this);
			t.withEnv(env);
			Preconditions.checkNotNull(t.getRebarGraph());
			return t;
		} catch (IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}

	
	public GraphDB getGraphDB() {
		return  graphWriter;
	}

	public String getScannerId() {
		return scannerId;
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

	public ScanQueue getScanQueue() {
		return queue;
	}
	
	public final EnvConfig getEnvConfig() {
		return env;
	}
}
