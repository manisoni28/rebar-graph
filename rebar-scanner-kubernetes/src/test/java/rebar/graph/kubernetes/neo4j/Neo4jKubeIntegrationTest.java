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
package rebar.graph.kubernetes.neo4j;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

import rebar.graph.core.GraphDB;
import rebar.graph.kubernetes.KubeIntegrationTest;
import rebar.graph.neo4j.GraphDriver;

public class Neo4jKubeIntegrationTest extends KubeIntegrationTest {

	
	public GraphDB getNeo4jDB() {
		return GraphDB.class.cast(getRebarGraph().getGraphDB());
	}
	public GraphDriver getNeo4jDriver() {
		return GraphDB.class.cast(getRebarGraph().getGraphDB()).getNeo4jDriver();
	}
	@BeforeEach
	public void assumeNeo4j() {
		Assumptions.assumeTrue(getRebarGraph().getGraphDB().getClass().getName().toLowerCase().contains("neo4j"));
		
		
		GraphDB neo4jDB = getNeo4jDB();
		
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeCluster)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubePod)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeService)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeEndpoints)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeNamespace)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeDeployment)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeContainer)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeReplicaSet)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeDaemonSet)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeNode)  detach delete a").exec();
		neo4jDB.getNeo4jDriver().cypher("match (a:KubeContainerSpec)  detach delete a").exec();
		
	}

}
