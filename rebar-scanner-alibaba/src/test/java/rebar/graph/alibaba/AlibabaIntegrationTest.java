package rebar.graph.alibaba;
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

import rebar.graph.test.AbstractIntegrationTest;

public class AlibabaIntegrationTest extends AbstractIntegrationTest {

	Logger logger = LoggerFactory.getLogger(AlibabaIntegrationTest.class);
	
	@Test
	public void testIt() throws Exception {
	
		try {
		
		AlibabaScanner scanner = getRebarGraph().newScanner(AlibabaScanner.class);
	

		scanner.scan();
		
		}
		catch (Exception e) {
			logger.debug("ignore",e);
		}
		
		
		
		
	}
}
