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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.test.AbstractIntegrationTest;

/**
 * Peform live integration tests against AWS.
 * 
 * @author rob
 *
 */
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AwsIntegrationTest extends AbstractIntegrationTest {

	static Logger logger = LoggerFactory.getLogger(AwsIntegrationTest.class);
	static Boolean awsAvailable = null;
	static AwsScanner awsScanner;

	
	public AwsIntegrationTest() {
		super();
	}


	@Override
	protected void beforeAll() {
		super.beforeAll();
		deleteAllAwsEntities();
	}


	boolean hasNodesOfType(AwsEntityType entityType) {
		return getNeo4jDriver().cypher("match (a:"+entityType+") return count(a) as count").findFirst().get().path("count").asInt()>0;
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


	protected AwsScanner getAwsScanner() {
		try {

			if (awsAvailable == null) {
				AwsScanner scanner = getRebarGraph().createBuilder(AwsScannerBuilder.class)
						.withRegion(Regions.US_WEST_2).withConfig(c -> {

						}).build();

				String account = scanner.getAccount();

				if (account != null) {
					logger.info("integration tests using AWS account: {}", account);
					awsAvailable = true;
					awsScanner = scanner;
				} else {
					awsAvailable = false;
				}
			}
		} catch (Exception e) {
			logger.warn("AWS integration tests will be skipped - " + e.toString());

			awsAvailable = false;
		}
		if (awsAvailable == null) {
			awsAvailable = false;
		}

		return awsScanner;
	}

	protected void assertSameAccountRegion(JsonNode a, JsonNode b) {
		Assertions.assertThat(a.path("account").asText()).isEqualTo(b.path("account").asText());
		Assertions.assertThat(a.path("region").asText()).isEqualTo(b.path("region").asText());

	}
}
