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
package rebar.graph.aws;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;

import rebar.graph.core.ScannerModule;
import rebar.graph.core.Main;
import rebar.util.Json;

public class AwsScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(AwsScannerModule.class);

	public static void main(String[] args) {
		Main.main(args);
	}

	public class FullScan implements Runnable {

		AwsScanner scanner;

		FullScan(AwsScanner scanner) {
			this.scanner = scanner;
		}

		
		public void markFullScanStart() {
			scanner.getGraphDB().getNeo4jDriver().	cypher("match (a:RebarScannerTarget {type:{type},target:{target},region:{region}}) set a.fullScanStartTs=timestamp() return a")
			.param("type", getScannerType())
			.param("target", scanner.getAccount())
			.param("region", scanner.getRegion().getName()).exec();
		}
		
		public void markFullScanEnd() {
			scanner.getGraphDB().getNeo4jDriver().	cypher("match (a:RebarScannerTarget {type:{type},target:{target},region:{region}}) set a.fullScanEndTs=timestamp() return a")
			.param("type", getScannerType())
			.param("target", scanner.getAccount())
			.param("region", scanner.getRegion().getName()).exec();
			
		}
		@Override
		public void run() {

			try {
				

				Optional<JsonNode> target = scanner.getGraphDB().getNeo4jDriver()
						.cypher("match (a:RebarScannerTarget {type:{type},target:{target},region:{region}}) return a")
						.param("type", getScannerType())
						.param("target", scanner.getAccount())
						.param("region", scanner.getRegion().getName())
						.findFirst();
				if (!target.isPresent()) {
					// we lost the entry, so re-register it
					registerScannerTarget(scanner.getAccount(), scanner.getRegion().getName());
					return;
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

	protected void scheduleRegion(String region) {
		AwsScannerBuilder b = getRebarGraph().createBuilder(AwsScannerBuilder.class);
		if (region != null) {
			b = b.withRegion(Regions.fromName(region));
		}
		Optional<String> assumeRole = getConfig().get("AWS_ROLE");
		if (assumeRole.isPresent()) {
			throw new UnsupportedOperationException("AWS_ROLE not yet supported");
		}
		AwsScanner scanner = b.build();

		registerScannerTarget(scanner.getAccount(), scanner.getRegion().getName());
		getExecutor().scheduleWithFixedDelay(new FullScan(scanner), 0, 10, TimeUnit.SECONDS);

	}

	public void init() {

		List<String> regions = Splitter.on(",; ").omitEmptyStrings().trimResults()
				.splitToList(getConfig().get("AWS_REGIONS").orElse(""));

		if (regions.isEmpty()) {
			// if regions were not specified in AWS_REGIONS, then just use the default
			// region per AWS SDK conventions
			scheduleRegion(null);
		} else {
			regions.forEach(region -> {
				scheduleRegion(region);
			});
		}

	}

}
