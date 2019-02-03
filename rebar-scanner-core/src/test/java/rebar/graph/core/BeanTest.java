package rebar.graph.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import rebar.graph.neo4j.GraphDriver;

public class BeanTest extends CoreIntegrationTest {

	@Autowired
	GraphDriver driver;
	
	@Autowired
	GraphDB graphDB;
	
	
	@Test
	public void testIt() {
	
		Assertions.assertThat(graphDB).isNotNull();
	}

}
