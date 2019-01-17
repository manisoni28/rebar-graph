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

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;
import rebar.graph.core.ScannerBuilder;
import rebar.graph.neo4j.GraphException;

public class KubeScannerBuilder extends ScannerBuilder<KubeScanner> {

	static Logger logger = LoggerFactory.getLogger(KubeScannerBuilder.class);
	// Supplier<KubernetesClient> clientSupplier = Suppliers.ofInstance(new
	// DefaultKubernetesClient());
	String clusterId = null;
	KubernetesClient client;

	File kubeConfig;
	String contextName;

	public KubeScannerBuilder withKubernetesClient(KubernetesClient client) {
		return withKubernetesClient(client, "default");
	}

	public KubeScannerBuilder withKubernetesClient(KubernetesClient client, String clusterId) {
		this.client = client;
		this.clusterId = clusterId;
		return this;
	}

	public KubeScannerBuilder withClusterId(String id) {
		this.clusterId = id;
		return this;
	}

	public KubeScannerBuilder withKubeConfigContext(File kubeConfig, String name) {
		this.kubeConfig = kubeConfig;
		this.contextName = name;
		this.clusterId = name;
		return this;
	}

	/**
	 * Find a stable clusterId. Oddly this is something that Kubernetes doesn't have
	 * natively.
	 * 
	 * @param client
	 * @param clusterId
	 * @param contextName
	 * @return
	 */
	@VisibleForTesting
	protected static String discoverClusterId(KubernetesClient client, String clusterId, String contextName) {

		if (!Strings.isNullOrEmpty(clusterId)) {
			logger.info("using explicitly specified clusterId: {}", clusterId);
			return clusterId;
		}
		
		if (!Strings.isNullOrEmpty(System.getenv("KUBE_CLUSTER_ID"))) {
			return System.getenv("KUBE_CLUSTER_ID");
		}
		try {
			// look in ConfigMap named 'rebar-graph' for a property named 'clusterId'

			ConfigMap m = client.configMaps().inNamespace("default").withName("rebar-graph").get();
			if (m != null) {

				String id = m.getData().get("clusterId");
				if (!Strings.isNullOrEmpty(id)) {
					logger.info("using clusterId from ConfigMap: {}", id);
					return id;
				}

			}
		} catch (RuntimeException e) {
			logger.info("could not read configmap: {}", "rebar-graph");
		}

		// If the contextName is set , use that
		if (!Strings.isNullOrEmpty(contextName)) {
			logger.info("using context name as clusterId: {}", contextName);
			return contextName;
		}

		// if contextName is null, let's look at the default context
		// No guarantee that this is actually what was used to configure the client, but
		// good enough.
		try {
			File f = new File(System.getProperty("user.home"), ".kube/config");
			if (f.exists()) {
				io.fabric8.kubernetes.api.model.Config fc = KubeConfigUtils.parseConfig(f);

				String id = fc.getCurrentContext();
				if (!Strings.isNullOrEmpty(id)) {
					logger.info("using current-context as clusterId: {}", id);
				}
				return id;

			}

		} catch (IOException e) {
			throw new GraphException(e);
		}

		
		throw new GraphException("could not determine clusterId");

	}

	@Override
	public KubeScanner build() {
		KubeScanner ks = new KubeScanner(this);

		if (client != null) {
			Preconditions.checkNotNull(clusterId);
			ks.client = client;
			ks.clusterId = discoverClusterId(client, this.clusterId, this.contextName);			
		} else {
			Config config = Config.autoConfigure(contextName);
			ks.client = new DefaultKubernetesClient(config);
			
			if (ks.client.getMasterUrl().toExternalForm().toLowerCase().contains(".eks.amazonaws.com")) {
				
				// We need to know the AMAZON cluster name for auth to work...need to enhance EKS to go find it.
				
				ks.client = EKS.newClientBuilder().withUrl(ks.client.getMasterUrl().toExternalForm()).build();
				
			}
			
			
			ks.clusterId = discoverClusterId(ks.client, this.clusterId, this.contextName);
		}
		logger.info("using clusterId: {}",ks.getClusterId());
		return ks;
	}

}
