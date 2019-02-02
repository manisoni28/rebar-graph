package rebar.graph.digitialocean;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountTest extends DigitalOceanIntegrationTest {

	public AccountTest() {

	}

	@Test
	public void testIt() {

		
		getGraphDriver().cypher("match (a) where labels(a)[0]=~'DigitalOcean.*' return a").forEach(it->{
			Assertions.assertThat(it.has("urn")).as("entity has urn: %s",it).isTrue();
			Assertions.assertThat(it.path("urn").asText()).as("urn follows naming convention: %s",it.toString()).startsWith("urn:digitalocean:cloud:");
		});

	}
}
