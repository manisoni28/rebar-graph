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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.amazonaws.regions.Regions;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rebar.graph.core.AbstractGraphModule;
import rebar.graph.core.Main;
import rebar.graph.core.RebarGraph;
import rebar.util.Sleep;

public class AwsGraphModule extends AbstractGraphModule {


	public static void main(String[] args) {
		Main.main(args);
	}
	
	
	private void fullScan(Regions region) {
		AwsScanner aws = getRebarGraph().createBuilder(AwsScannerBuilder.class).build();
		
	
		aws.getEntityScanner(AllEntityScanner.class).scan();
	}
	public void run() {

		
	
		List<String> regions = Splitter.on(",; ").omitEmptyStrings().trimResults().splitToList(getConfig().get("AWS_REGIONS").orElse(""));
		if (regions.size()>0) {
			throw new UnsupportedOperationException("AWS_REGIONS not yet supported");
		}
		
		Optional<String> assumeRole = getConfig().get("AWS_ROLE");
		if (assumeRole.isPresent()) {
			throw new UnsupportedOperationException("AWS_ROLE not yet supported");
		}
		
		
		
		if (isFullScanEnabled()) {
			Runnable r = new Runnable() {

				@Override
				public void run() {
					fullScan(null);
					
				}
				
			};
			getExecutor().scheduleWithFixedDelay(r, 0, getFullScanInterval(), TimeUnit.SECONDS);
		}
		else {
			fullScan(null);
		}
		


	}
	
	
}
