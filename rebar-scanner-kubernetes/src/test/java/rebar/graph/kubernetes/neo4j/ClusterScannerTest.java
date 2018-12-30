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

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.core.GraphDB;
import rebar.graph.core.Neo4jGraphDB;
import rebar.graph.kubernetes.KubeScanner;
import rebar.graph.kubernetes.KubernetesScannerBuilder;
import rebar.util.Json;

public class ClusterScannerTest extends Neo4jKubeIntegrationTest {

	@Test
	public void testX() {

		KubeScanner scanner = getRebarGraph().createBuilder(KubernetesScannerBuilder.class)
				.withKubernetesClient(getKubeScanner().getKubernetesClient()).register("foo");

		Assertions.assertThat(scanner).isNotNull();
		Assertions.assertThat(getRebarGraph().getScanner(KubeScanner.class, "foo")).isSameAs(scanner);
	}

	@Test
	public void testCleanup() throws InterruptedException {

		Assertions.assertThat(
				getNeo4jDriver().cypher("match (a) where a.graphEntityGroup='kubernetes' return count(a) as count")
						.stream().findFirst().get().path("count").asInt())
				.isEqualTo(0);

		Assertions.assertThat(getNeo4jDriver().cypher("match (a) where labels(a)[0]=~'Kube.*' return count(a) as count")
				.stream().findFirst().get().path("count").asInt()).isEqualTo(0);
	}

	@Test
	public void testEntityAttributes() {
		getNeo4jDriver().cypher("match (a) where labels(a)[0] =~ 'Kube.*' return labels(a)[0] as label,a").stream()
		.forEach(it -> {

			Assertions.assertThat(it.path("a").path("graphEntityType").asText()).startsWith("Kube");
			Assertions.assertThat(it.path("a").path("graphEntityGroup").asText()).isEqualTo("kubernetes");
		});
	}
	@Test
	public void testIt() throws InterruptedException {


		getKubeScanner().scan();

		List<JsonNode> list = getNeo4jDriver().cypher("match (a:KubeCluster) return a").list();

		Assertions.assertThat(list).isNotEmpty();

		list.forEach(it -> {
			Assertions.assertThat(it.path("masterUrl").asText()).startsWith("http");
		});

	
	}
}
