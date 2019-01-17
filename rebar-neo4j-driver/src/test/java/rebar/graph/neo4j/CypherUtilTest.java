package rebar.graph.neo4j;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import rebar.graph.neo4j.CypherUtil;

public class CypherUtilTest {

	
	
	@Test
	public void testEscapePropertyName() {
		Assertions.assertThat(CypherUtil.escapePropertyName("fizz-buzz")).isEqualTo("`fizz-buzz`");
		Assertions.assertThat(CypherUtil.escapePropertyName("foo")).isEqualTo("`foo`");
	}

}
