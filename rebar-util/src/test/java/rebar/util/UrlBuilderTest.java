package rebar.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import rebar.util.UrlBuilder;

public class UrlBuilderTest {

	
	@Test
	public void testIt() {
		Assertions.assertThat(new UrlBuilder().url("https://www.example.com/{fizz}").pathParam("fizz", "buzz").toString()).isEqualTo("https://www.example.com/buzz");
		Assertions.assertThat(new UrlBuilder().url("https://www.example.com/{fizz}").pathParam("fizz", "buzz").queryParam("a", "b").toString()).isEqualTo("https://www.example.com/buzz?a=b");
	
		Assertions.assertThat(new UrlBuilder().url("https://www.example.com/a%20b").toString()).isEqualTo("https://www.example.com/a+b");
		
		
		Assertions.assertThat(new UrlBuilder().url("https://www.google.com/").path("f %&b").toString()).isEqualTo("https://www.google.com/f+%25%26b");
	}
}
