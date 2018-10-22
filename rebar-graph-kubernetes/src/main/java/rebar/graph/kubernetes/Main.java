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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.zjsonpatch.internal.guava.Strings;
import rebar.graph.core.RebarGraph;

public class Main {

	public static void main(String[] args) {

		Logger logger = LoggerFactory.getLogger(Main.class);
		
		
		
		
		RebarGraph g = new RebarGraph.Builder().build();
		
		KubeScanner scanner = g.createBuilder(KubernetesScannerBuilder.class).withClusterId("foo").build();
		scanner.watchEvents();
		while (true == true) {
			try {
				scanner.scan();

			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(60000);
			}
			catch (Exception e) {}
		}

		
	}

}
