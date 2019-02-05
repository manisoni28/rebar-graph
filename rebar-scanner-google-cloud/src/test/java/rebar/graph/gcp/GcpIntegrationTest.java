package rebar.graph.gcp;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
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

import com.google.common.base.Preconditions;

import rebar.graph.gcp.GcpScanner;
import rebar.graph.test.AbstractIntegrationTest;

public class GcpIntegrationTest extends AbstractIntegrationTest {

		
	static GcpScanner scanner;
	static boolean skipAll=false;
	@Override
	protected void beforeAll() {
		
		
		super.beforeAll();
		
	
		int count = getGraphDriver().cypher("match (a) where labels(a)[0]=~'Gcp.*' detach delete a return count(a) as count").findFirst().get().path("count").asInt();
		logger.info("deleted {} Gcp nodes",count);
		checkAccess();
		getScanner().scan();
	}

	Logger logger = LoggerFactory.getLogger(GcpIntegrationTest.class);
	
	@BeforeEach
	private void checkAccess() {
		try {
			if (scanner==null) {
				scanner = getRebarGraph().newScanner(GcpScanner.class);
				scanner.projectScanner().scan();
				skipAll=false;
			}
		
		}
		catch (Exception e) {
			logger.info("GCP integration tests will be disabled - "+e.toString());
			skipAll = true;
		}
		Assumptions.assumeTrue(scanner!=null && (!skipAll));
	}
	public GcpScanner getScanner() {
		Preconditions.checkState(scanner!=null,"scanner not initialized");
		return scanner;
	}
	
	
	@Test
	public void testIt() throws Exception {
	
		
		getRebarGraph().newScanner(GcpScanner.class).zoneScanner().scan();
		
		
	
		
	}
}
