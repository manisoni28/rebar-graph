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


import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.v1.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.driver.GraphDriver;
import rebar.graph.neo4j.Neo4jDriver;

class Neo4jIntegrationTest {


	static Logger logger = LoggerFactory.getLogger(Neo4jIntegrationTest.class);
	static Boolean neo4jAvailable;

	static Neo4jDriver driver;

	static RebarGraph rebarGraph;
	
	public Neo4jDriver getNeo4jDriver() {
		return driver;
	}

	public RebarGraph getRebarGraph() {
		return rebarGraph;
	}
	
	String getDefaultNeo4jUrl() {
		return "bolt://localhost:7687";
	}
	protected Optional<String> getTestProperty(String key) {

		key = key.toLowerCase().replace("_", ".");

		String val = System.getProperty(key);

		if (val != null) {
			return Optional.of(val);
		}

		key = key.toUpperCase().replace('.', '_');

		val = System.getenv(key);

		if (val != null) {
			return Optional.of(val);
		}

		return Optional.empty();

	}
	
	@BeforeEach
	public void setupNeo4j() {
		if (neo4jAvailable == null) {
			try {


				String tryUrl = getTestProperty(GraphDriver.GRAPH_URL).orElse(getDefaultNeo4jUrl());
				Optional<String> username = getTestProperty(GraphDriver.GRAPH_USERNAME);
				Optional<String> password = getTestProperty(GraphDriver.GRAPH_PASSWORD);
				logger.info("trying {}",tryUrl);
				GraphDriver.Builder builder = new GraphDriver.Builder().withUrl(tryUrl);
				if (username.isPresent()) {
					builder = builder.withUsername(username.get());
				}
				if (password.isPresent()) {
					builder = builder.withPassword(password.get());
				}
				driver = (Neo4jDriver) builder.build();
				try (Session session = driver.getDriver().session()) {
					session.run("match (a:RebarHealthCheck) return a limit 1").consume();
					neo4jAvailable = true;
					this.rebarGraph = new RebarGraph.Builder().withGraphDB(new Neo4jGraphDB(driver)).build();
				}
			} catch (Exception e) {
				logger.info("neo4j not available", e);
				driver = null;
				neo4jAvailable = false;
			}
		}

		Assumptions.assumeTrue(neo4jAvailable);

		getNeo4jDriver().newTemplate().cypher("match (a) return distinct labels(a)[0] as label").stream()
				.map(x -> x.path("label").asText()).distinct().filter(p -> p.toLowerCase().startsWith("junit") || p.toLowerCase().startsWith("test"))
				.forEach(it -> {
					logger.info("deleting nodes with label: {}", it);
					getNeo4jDriver().newTemplate().cypher("match (a:" + it + ") detach delete a").exec();
				});

	}
}
