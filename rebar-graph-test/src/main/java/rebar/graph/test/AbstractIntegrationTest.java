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
package rebar.graph.test;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.core.Neo4jGraphDB;
import rebar.graph.core.RebarGraph;
import rebar.graph.neo4j.Neo4jDriver;



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

		try {
			RebarGraph graph = new RebarGraph.Builder().build();
			
			graph.getGraphDB().nodes().label("JUnitTest").match().forEach(it->{
			//	System.out.println(">> "+it);
			});

	
			rebarGraph = graph;
		
			cleanupTestData();
		} catch (Exception e) {

			e.printStackTrace();
			logger.warn("could not connect to {} ... provider will be blacklisted");

			Assumptions.assumeTrue(false);
		}

	}

	private void cleanupTestData() {
		Neo4jDriver driver = Neo4jGraphDB.class.cast(getRebarGraph().getGraphDB()).getNeo4jDriver();
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