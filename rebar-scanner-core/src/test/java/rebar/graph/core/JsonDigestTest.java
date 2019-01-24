package rebar.graph.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import rebar.util.Json;

public class JsonDigestTest {

	@Test
	public void testSort() {
		
		
		JsonDigest digest = new JsonDigest();
		
		Assertions.assertThat(digest.digest(Json.objectNode().put("fizz", "buzz").put("foo", "bar"))).isEqualTo(digest.digest(Json.objectNode().put("foo", "bar").put("fizz", "buzz")));
	}
	
	@Test
	public void testTypeDifferentiation() {
		JsonDigest digest = new JsonDigest();
		Assertions.assertThat(digest.digest(Json.objectNode().put("foo", 1))).isNotEqualTo(digest.digest(Json.objectNode().put("foo", "1")));
	}

}
