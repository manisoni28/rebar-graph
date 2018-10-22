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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.aws.AllEntityScanner;
import rebar.graph.aws.AwsScanner;
import rebar.graph.aws.AwsScannerBuilder;
import rebar.graph.core.RebarGraph;
import rebar.graph.kubernetes.KubeScanner;
import rebar.graph.kubernetes.KubernetesScannerBuilder;

public class AwsRunner implements Runnable {

	Logger logger = LoggerFactory.getLogger(KubeRunner.class);

	RebarGraph graph;

	public AwsRunner(RebarGraph g) {
		this.graph = g;
	}

	@Override
	public void run() {
		while (true == true) {
			try {
				File f = new File(System.getenv("HOME"), ".aws/config");
				if (f.exists()) {
					AwsScanner scanner = graph.createBuilder(AwsScannerBuilder.class).build();
					scanner.getScanner(AllEntityScanner.class).scan();
				}

			} catch (Exception e) {
				logger.warn("problem", e);

			}
			try {
				Thread.sleep(60000L);
			} catch (Exception x) {
			}
		}

	}

}
