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
import com.microsoft.azure.management.resources.Subscription;

import rebar.graph.azure.AzureScanner;
import rebar.graph.test.AbstractIntegrationTest;

public abstract class AzureIntegrationTest extends AbstractIntegrationTest {

		
	static AzureScanner scanner;
	static boolean skipAll=false;
	@Override
	protected void beforeAll() {
		
		
		super.beforeAll();
		
	
		int count = getGraphDriver().cypher("match (a) where labels(a)[0]=~'Azure.*' detach delete a return count(a) as count").findFirst().get().path("count").asInt();
		logger.info("deleted {} Azure nodes",count);
		checkAccess();
	
	}

	Logger logger = LoggerFactory.getLogger(AzureIntegrationTest.class);
	
	@BeforeEach
	private void checkAccess() {
		try {
			if (scanner==null) {
				scanner = getRebarGraph().newScanner(AzureScanner.class);
				Subscription sub = scanner.getAzureClient().getCurrentSubscription();
				skipAll=false;
			}
		
		}
		catch (Exception e) {
			logger.info("Azure integration tests will be disabled - "+e.toString());
			skipAll = true;
		}
		Assumptions.assumeTrue(scanner!=null && (!skipAll));
	}
	public AzureScanner getScanner() {
		Preconditions.checkState(scanner!=null,"scanner not initialized");
		return scanner;
	}
	
	

}
