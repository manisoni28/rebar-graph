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
package rebar.graph.aws;

import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;

import rebar.graph.test.AbstractIntegrationTest;

/**
 * Peform live integration tests against AWS.
 * 
 * @author rob
 *
 */

public abstract class AwsIntegrationTest extends AbstractIntegrationTest {

	static Logger logger = LoggerFactory.getLogger(AwsIntegrationTest.class);

	static AwsScanner awsScanner;
	static AtomicInteger failureCount = new AtomicInteger();
	
	public AwsIntegrationTest() {
		super();
	}

	
	@Override
	protected void checkAssumptions() {
		Assumptions.assumeTrue(getAwsScanner()!=null);
	}
	@Override
	protected void beforeAll() {
		super.beforeAll();
		deleteAllAwsEntities();
		
		Assumptions.assumeTrue(getAwsScanner()!=null);
	}


	boolean hasNodesOfType(AwsEntityType entityType) {
		return getGraphDriver().cypher("match (a:"+entityType+") return count(a) as count").findFirst().get().path("count").asInt()>0;
	}
	@BeforeEach
	public void __setupAws() {

		Assumptions.assumeTrue(getAwsScanner()!=null);
	}

	protected void deleteAllAwsEntities() {
	
		logger.info("deleting all Aws nodes from graph...");
		getRebarGraph().getGraphDB().getNeo4jDriver()
		.cypher("match (a) where labels(a)[0]=~'Aws.*' detach delete a").exec();
	
	}

	@BeforeEach
	public void checkAccess() {
		Assumptions.assumeTrue(getAwsScanner()!=null);
	}
	protected AwsScanner getAwsScanner() {
		try {
			if (awsScanner!=null) {
				return awsScanner;
			}
			if (failureCount.get()==0) {
				AwsScanner scanner = getRebarGraph().newScanner(AwsScanner.class,ImmutableMap.of("region",Regions.US_WEST_2.getName()));//

				String account = scanner.getAccount();

				if (account != null) {
					logger.info("integration tests using AWS account: {}", account);
				
					awsScanner = scanner;
				} else {
					failureCount.incrementAndGet();
				}
			}
		} catch (Exception e) {
			logger.warn("AWS integration tests will be skipped",e);

			failureCount.incrementAndGet();
		}
		

		return awsScanner;
	}

	protected void assertSameAccountRegion(JsonNode a, JsonNode b) {
		Assertions.assertThat(a.path("account").asText()).isEqualTo(b.path("account").asText());
		Assertions.assertThat(a.path("region").asText()).isEqualTo(b.path("region").asText());

	}
}
