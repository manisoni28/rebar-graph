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
package rebar.graph.docker;

import static rebar.util.Sleep.sleep;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.machinezoo.noexception.Exceptions;

import rebar.graph.core.ScannerModule;
import rebar.util.Sleep;

public class DockerScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(DockerScannerModule.class);

	@Override
	public void init() {

		while (true == true) {
			Exceptions.log(logger).run(() -> {
				logger.info("running docker scanner...");
			
				sleep(30,TimeUnit.SECONDS);
			});

		}

	}

}
