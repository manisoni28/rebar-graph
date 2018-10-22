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
package rebar.graph.demo;

import rebar.graph.core.RebarGraph;
import rebar.graph.kubernetes.KubeScanner;
import rebar.graph.kubernetes.KubernetesScannerBuilder;

public class Main {

	public static void main(String[] args) throws Exception {

		RebarGraph g = new RebarGraph.Builder().build();
		
		KubeScanner scanner = g.createBuilder(KubernetesScannerBuilder.class).build();
		
		System.out.println(scanner.getClusterId());
		new Thread(new KubeRunner(g)).start();
		new Thread(new AwsRunner(g)).start();

	}
}
