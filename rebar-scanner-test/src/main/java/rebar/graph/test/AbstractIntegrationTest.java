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
package rebar.graph.test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Sets;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import rebar.graph.core.BaseConfig;
import rebar.graph.core.GraphBuilder;
import rebar.graph.core.RebarGraph;
import rebar.graph.neo4j.GraphDriver;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(classes = { BaseConfig.class })
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

	
	static Set<Class> beforeInvoked = Sets.newHashSet();
	static AtomicBoolean loggingRegistryAdded = new AtomicBoolean(false);
	Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);

	@Autowired
	RebarGraph rebarGraph;

	static AtomicInteger failureCount = new AtomicInteger(0);
	/**
	 * Adds logging registry, used primarily for testing.
	 */
	public static void enableLoggingRegistry() {
		Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);
		synchronized (Metrics.globalRegistry) {
			long logRegCount = Metrics.globalRegistry.getRegistries().stream()
					.filter(p -> p instanceof LoggingMeterRegistry).count();
			if (logRegCount == 0) {
				logger.info("adding LoggingMeterRegistry to micrometer...");

				Metrics.addRegistry(new LoggingMeterRegistry());

		
			
			}
		}
	}

	public AbstractIntegrationTest() {

		enableLoggingRegistry();

	}

	@BeforeEach
	private final void checkNeo4j() {
		try {
			Assumptions.assumeTrue(failureCount.get()<3);
			getGraphDriver().cypher("match (a:ConnectivityCheck) return a limit 1").findFirst();
		}
		catch (RuntimeException e) {
			if (isNeo4jRequired()) {
				throw e;
			}
			Assumptions.assumeTrue(false,e.toString());
		}
		
	
	}
	
	/**
	 * We use @BeforeEach to implement beforeAll() because assumptions don't work correctly in @BeforeAll.
	 */
	@BeforeEach
	private final void internalBeforeAll() {
		checkNeo4j();
		checkAssumptions();
		if (beforeInvoked.contains(getClass())) {
			return;
		}
		
		beforeInvoked.add(getClass());
		logger.info("invoking beforeAll()...");
		
		beforeAll();
	}

	protected void checkAssumptions() {
		
	}
	protected void beforeAll() {

	}

	/**
	 * By setting NEO4J_REQUIRED=true, we prevent tests from being skipped. We set
	 * this to true in the CI environment so that all builds run against NEO4J.
	 * 
	 * @return
	 */
	public boolean isNeo4jRequired() {

		return Boolean.parseBoolean(System.getenv("NEO4J_REQUIRED"));
	}

	@BeforeEach
	protected final void __setupRebarGraph() {

		Assumptions.assumeTrue(getRebarGraph() != null);

	}

	protected void cleanupTestData() {
		GraphDriver driver = getRebarGraph().getGraphBuilder().getNeo4jDriver();
		driver.newTemplate().cypher("match (a) where exists (a.testData) detach delete a").list();
		driver.newTemplate().cypher("match (a) return distinct labels(a)[0] as label").stream()
				.map(x -> x.path("label").asText()).distinct()
				.filter(p -> p.toLowerCase().startsWith("junit") || p.toLowerCase().startsWith("test")).forEach(it -> {
					logger.info("deleting nodes with label: {}", it);
					driver.newTemplate().cypher("match (a:" + it + ") detach delete a").exec();
				});
	}

	public final RebarGraph getRebarGraph() {
		return rebarGraph;
	}
	public final GraphBuilder getGraphBuilder() {
		return getRebarGraph().getGraphBuilder();
	}
	public final GraphDriver getGraphDriver() {
		return getRebarGraph().getGraphBuilder().getNeo4jDriver();
	}

}
