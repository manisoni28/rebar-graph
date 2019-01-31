package rebar.graph.digitialocean;

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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.digitalocean.DigitalOceanScanner;
import rebar.graph.digitalocean.DigitalOceanScannerBuilder;
import rebar.graph.test.AbstractIntegrationTest;

public class DigitialOceanIntegrationTest extends AbstractIntegrationTest {

	@Override
	protected void beforeAll() {
		super.beforeAll();
		getGraphDriver().cypher("match (a) where labels(a)[0]=~'DigitalOcean.*' detach delete a").exec();
	}

	Logger logger = LoggerFactory.getLogger(DigitialOceanIntegrationTest.class);

	@Test
	public void testIt() throws Exception {

		try {

			DigitalOceanScanner scanner = getRebarGraph().createBuilder(DigitalOceanScannerBuilder.class).build();

			scanner.scan();

		} catch (Exception e) {
			logger.debug("ignore", e);
		}

	}
}
