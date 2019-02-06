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
package rebar.graph.docker;

import static rebar.util.Sleep.sleep;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.machinezoo.noexception.Exceptions;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;
import rebar.graph.core.Main;
import rebar.graph.core.ScannerModule;
import rebar.util.Sleep;

@Component
public class DockerScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(DockerScannerModule.class);

	DockerScanner scanner;

	private void eventLoop() {
		while (true == true) {
			try {
				scanner.getDockerClient().events().forEachRemaining(it -> {

					logger.info("action={} id={}",it.action(),it.actor().id());

				});
				;
			} catch (Exception e) {
				logger.warn("event loop problem", e);
			}
			// should never get here, but if we do, slow down so we don't crash-loop
			Sleep.sleep(500, TimeUnit.SECONDS);
		}
	}

	@Override
	public void doStartModule() {
		scanner = getRebarGraph().newScanner(DockerScanner.class);

		new ThreadFactoryBuilder().setDaemon(true).setNameFormat("evt-%s").build().newThread(this::eventLoop).run();
	
		while (true == true) {
			Exceptions.log(logger).run(() -> {
				logger.info("running docker scanner...");
				scanner.scan();

			});
			sleep(30, TimeUnit.SECONDS);
		}

	}

	public static void main(String[] args) throws Exception {
		Main.main(args);
	}
	
	@Override
	public void applyConstraints(boolean apply) {
		getRebarGraph().getGraphBuilder().schema().createUniqueConstraint(DockerEntityType.DockerContainer.name(), "urn",apply);
		getRebarGraph().getGraphBuilder().schema().createUniqueConstraint(DockerEntityType.DockerHost.name(), "urn",apply);
	
	}
}
