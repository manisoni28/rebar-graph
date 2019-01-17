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

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import rebar.graph.core.ScannerModule;
import rebar.graph.core.Main;

public class KubeScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(KubeScannerModule.class);

	KubeScanner scanner;

	public static void main(String[] args) {
		Main.main(args);
	}

	private void scan() {
		try {
			
			scanner.scan();
		} catch (Exception e) {
			logger.warn("problem", e);
		}
	}

	public class FullScan implements Runnable {

		KubeScanner scanner;

		FullScan(KubeScanner scanner) {
			this.scanner = scanner;
		}

		
		public void markFullScanStart() {
			scanner.getRebarGraph().getGraphDB().getNeo4jDriver().	cypher("match (a:RebarScannerTarget {type:{type},target:{target},region:{region}}) set a.fullScanStartTs=timestamp() return a")
			.param("type", getScannerType())
			.param("target", scanner.getClusterId())
			.param("region", "undefined").exec();
		}
		
		public void markFullScanEnd() {
			scanner.getRebarGraph().getGraphDB().getNeo4jDriver().	cypher("match (a:RebarScannerTarget {type:{type},target:{target},region:{region}}) set a.fullScanEndTs=timestamp() return a")
			.param("type", getScannerType())
			.param("target", scanner.getClusterId())
			.param("region", "undefined").exec();
			
		}
		@Override
		public void run() {

			try {
				
				Optional<JsonNode> target = scanner.getGraphDB().getNeo4jDriver()
						.cypher("match (a:RebarScannerTarget {type:{type},target:{target},region:{region}}) return a")
						.param("type", getScannerType())
						.param("target", scanner.getClusterId())
						.param("region", "undefined")
						.findFirst();
				if (!target.isPresent()) {
					// we lost the entry, so re-register it
					registerScannerTarget(scanner.getClusterId(), "undefined");
					target = scanner.getGraphDB().getNeo4jDriver()
							.cypher("match (a:RebarScannerTarget {type:{type},target:{target},region:{region}}) return a")
							.param("type", getScannerType())
							.param("target", scanner.getClusterId())
							.param("region", "undefined")
							.findFirst();
				
				}

				long lastFullScanStartTs = target.get().path("fullScanStartTs").asLong(0);
				long fullScanIntervalMillis = TimeUnit.SECONDS.toMillis(target.get().path("fullScanIntervalSecs").asLong(300L));
				boolean fullScanEnabled = target.get().path("fullScanEnabled").asBoolean(true);
				
				if (!fullScanEnabled) {
					logger.info("full scan is disabled...noop");
					return;
				}
				
				if (System.currentTimeMillis() < lastFullScanStartTs+fullScanIntervalMillis) {
					logger.info("last full scan started at {} ... another will not start until {}",new Date(lastFullScanStartTs),new Date(lastFullScanStartTs+fullScanIntervalMillis));
					return;
				}
				markFullScanStart();
				scanner.scan();
				markFullScanEnd();
			} catch (Exception e) {
				logger.warn("unexpected exception", e);
			}

		}

	}
	public void init() {

		if (scanner == null) {
			
		
			
			scanner = getRebarGraph().createBuilder(KubeScannerBuilder.class).build();
			scanner.applyConstraints();
			scanner.watchEvents(); // idempotent
		}
		
		getExecutor().scheduleWithFixedDelay(new FullScan(scanner), 0, 15, TimeUnit.SECONDS);
	}

}
