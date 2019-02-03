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
package rebar.graph.kubernetes;

import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import rebar.graph.core.GraphDB;
import rebar.graph.neo4j.GraphDriver;
import rebar.graph.test.AbstractIntegrationTest;

public abstract class KubeIntegrationTest extends AbstractIntegrationTest {

	static boolean integrationTestDisabled = false;
	static KubeScanner kubeScanner = null;

	static Logger logger = LoggerFactory.getLogger(KubeIntegrationTest.class);

	@BeforeEach
	protected void setupKube() {
		if (kubeScanner != null) {
			return;
		}
		if (integrationTestDisabled) {
			Assumptions.assumeFalse(integrationTestDisabled, "kube not available");
			return;
		}

		try {
			KubeScanner s = getRebarGraph().newScanner(KubeScanner.class);
			s.getKubernetesClient().getVersion().getBuildDate();
			kubeScanner = s;

		} catch (Exception e) {
			logger.warn("Kubernetes integration tests will be skipped: " + e.toString());
			integrationTestDisabled = true;
		}

		Assumptions.assumeTrue(!integrationTestDisabled);
		Assumptions.assumeTrue(kubeScanner!=null);
	}

	public boolean isKubernetesAvailable() {
		return kubeScanner!=null;
	}

	public KubernetesClient getKubernetesClient() {
		return kubeScanner.getKubernetesClient();
	}

	public KubeScanner getKubeScanner() {

		return kubeScanner;
	}

	public GraphDB getNeo4jDB() {
		return GraphDB.class.cast(getRebarGraph().getGraphDB());
	}


	@BeforeEach
	public void assumeNeo4j() {

		GraphDB neo4jDB = getNeo4jDB();
		logger.info("deleting all Kube ndoes before test");
		neo4jDB.getNeo4jDriver().cypher("match (a) where labels(a)[0]=~'Kube.*'  detach delete a").exec();

	}
}
