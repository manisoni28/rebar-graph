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
package rebar.graph.test;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.core.RebarGraph;
import rebar.graph.neo4j.GraphDriver;
import rebar.util.EnvConfig;



public abstract class AbstractIntegrationTest {

	Logger logger = LoggerFactory.getLogger(getClass());

	private static RebarGraph rebarGraph;

	public AbstractIntegrationTest() {

	}

	@BeforeEach
	public void setup() {

		if (this.rebarGraph!=null) {
			return;
		}

		EnvConfig checkConfig = new EnvConfig();
		
	
		try {
			RebarGraph.Builder b = new RebarGraph.Builder();
			
			if (!new EnvConfig().get("GRAPH_URL").isPresent()) {
				LoggerFactory.getLogger(AbstractIntegrationTest.class).info("GRAPH_URL not set ... defaulting to bolt://localhost:7687");
				b = b.withGraphUrl("bolt://localhost:7687");
			}
			
			RebarGraph graph = b.build();
			
			graph.getGraphDB().nodes("JUnitTest").match().forEach(it->{
			
			});

	
			rebarGraph = graph;
		
			cleanupTestData();
		} catch (Exception e) {

	
			logger.warn("could not connect to neo4j",e);

			Assumptions.assumeTrue(false);
		}

	}

	private void cleanupTestData() {
		GraphDriver driver = getRebarGraph().getGraphDB().getNeo4jDriver();
		driver.newTemplate().cypher("match (a) where exists (a.testData) detach delete a").list();
		driver.newTemplate().cypher("match (a) return distinct labels(a)[0] as label").stream()
		.map(x -> x.path("label").asText()).distinct().filter(p -> p.toLowerCase().startsWith("junit") || p.toLowerCase().startsWith("test"))
		.forEach(it -> {
			logger.info("deleting nodes with label: {}", it);
			driver.newTemplate().cypher("match (a:" + it + ") detach delete a").exec();
		});
	}
	public RebarGraph getRebarGraph() {
		return rebarGraph;
	}

}
