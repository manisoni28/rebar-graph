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
package rebar.graph.kubernetes;

import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import rebar.graph.test.AbstractIntegrationTest;

public abstract class KubeIntegrationTest extends AbstractIntegrationTest {

	static Optional<KubernetesClient> kubernetesClient;

	static KubeScanner kubeScanner = null;

	static Logger logger = LoggerFactory.getLogger(KubeIntegrationTest.class);
	
	@BeforeEach
	protected void setupKube() {
		try {
			if (kubernetesClient == null) {
				kubernetesClient = Optional.of(new DefaultKubernetesClient());
				kubernetesClient.get().getVersion().getBuildDate();
			}
		} catch (Exception e) {
			logger.warn("Kubernetes integration tests will be skipped: "+e.toString());
			kubernetesClient = Optional.empty();
		}

		Assumptions.assumeTrue(kubernetesClient != null && kubernetesClient.isPresent());
	}

	public boolean isKubernetesAvailable() {
		return kubernetesClient != null && kubernetesClient.isPresent();
	}

	public KubernetesClient getKubernetesClient() {
		return kubernetesClient.orElse(null);
	}

	public KubeScanner getKubeScanner() {

		if (kubeScanner!=null) {
			return kubeScanner;
		}
		KubeScanner ks = getRebarGraph().createBuilder(KubernetesScannerBuilder.class)
				.withKubernetesClient(kubernetesClient.get()).build();
		this.kubeScanner = ks;
		return ks;
	}

}
