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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.views.AbstractView;

import rebar.graph.core.AbstractGraphModule;
import rebar.graph.core.Main;
import rebar.graph.core.RebarGraph;

public class KubernetesGraphModule extends AbstractGraphModule {

	Logger logger = LoggerFactory.getLogger(KubernetesGraphModule.class);

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

	public void run() {

		if (scanner == null) {
			scanner = getRebarGraph().createBuilder(KubernetesScannerBuilder.class).build();
			scanner.applyConstraints();
			scanner.watchEvents(); // idempotent
		}
		
		if (isFullScanEnabled()) {
			getExecutor().scheduleWithFixedDelay(this::scan, 0, getFullScanInterval(), TimeUnit.SECONDS);
		}
		else {
			getExecutor().execute(this::scan);
		}

	}

}
