package rebar.graph.core;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.v1.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import rebar.graph.config.CoreSpringConfig;
import rebar.graph.neo4j.GraphDriver;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(classes= {BaseConfig.class})
@TestInstance(Lifecycle.PER_CLASS)
public abstract class CoreIntegrationTest {

	static Logger logger = LoggerFactory.getLogger(CoreIntegrationTest.class);
	@Autowired
	GraphDriver driver;
	
	@Autowired
	RebarGraph rebarGraph;
	
	// counting failures allows us to fail fast when neo4j isn't available
	static AtomicInteger failureCount = new AtomicInteger();
	
	@BeforeEach
	public void setupCore() {
		try {
			
			Assumptions.assumeTrue(failureCount.get()<3,"could not connect to neo4j");
			
			driver.newTemplate().cypher("match (a) return distinct labels(a)[0] as label").stream()
			.map(x -> x.path("label").asText()).distinct().filter(p -> p.toLowerCase().startsWith("junit") || p.toLowerCase().startsWith("test"))
			.forEach(it -> {
				logger.info("deleting nodes with label: {}", it);
				driver.newTemplate().cypher("match (a:" + it + ") detach delete a").exec();
			});
		}
		catch (Exception e) {
			failureCount.getAndIncrement();
			Assumptions.assumeTrue(false,"could not connect to neo4j - "+e.toString());
		}
	}

	public GraphDriver getNeo4jDriver() {
		return driver;
	}

	public RebarGraph getRebarGraph() {
		return rebarGraph;
	}
}
