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
package rebar.graph.neo4j;

import static rebar.graph.neo4j.GraphDriver.GRAPH_PASSWORD;
import static rebar.graph.neo4j.GraphDriver.GRAPH_URL;
import static rebar.graph.neo4j.GraphDriver.GRAPH_USERNAME;

import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.v1.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class IntegrationTest {

	static Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
	static Boolean neo4jAvailable;
	static String url = null;
	static Neo4jDriverImpl provider;

	public Neo4jDriverImpl getNeo4jDriver() {
		return provider;
	}

	String getUrl() {
		return url;
	}

	public void createMovieGraph() {
		new MovieGraph(getNeo4jDriver()).replaceMovieGraph();
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

	String getDefaultNeo4jUrl() {
		return "bolt://localhost:7687";
	}

	@BeforeEach
	public void setupNeo4j() {
		if (neo4jAvailable == null) {
			try {

				String tryUrl = getTestProperty(GRAPH_URL).orElse(getDefaultNeo4jUrl());
				Optional<String> username = getTestProperty(GRAPH_USERNAME);
				Optional<String> password = getTestProperty(GRAPH_PASSWORD);
				logger.info("trying {}", tryUrl);
				GraphDriver.Builder builder = new GraphDriver.Builder().withUrl(tryUrl);
				if (username.isPresent()) {
					builder = builder.withUsername(username.get());
				}
				if (password.isPresent()) {
					builder = builder.withPassword(password.get());
				}
				provider = (Neo4jDriverImpl) builder.build();
				try (Session session = provider.getDriver().session()) {
					session.run("match (a:RebarHealthCheck) return a limit 1").consume();
					neo4jAvailable = true;
					url = tryUrl;
				}
			} catch (Exception e) {
				logger.info("neo4j not available", e);
				provider = null;
				neo4jAvailable = false;
			}
		}

		Assumptions.assumeTrue(neo4jAvailable);

		getNeo4jDriver().newTemplate().cypher("match (a) return distinct labels(a)[0] as label").stream()
				.map(x -> x.path("label").asText()).distinct().filter(p -> p.toLowerCase().startsWith("junit"))
				.forEach(it -> {
					logger.info("deleting nodes with label: {}", it);
					getNeo4jDriver().newTemplate().cypher("match (a:" + it + ") detach delete a").exec();
				});
		new MovieGraph(getNeo4jDriver()).deleteMovieGraph();

	}
}
