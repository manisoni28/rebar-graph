package rebar.graph.digitialocean;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import rebar.graph.digitalocean.DigitalOceanEntityScanner;

public class DigitalOceanEntityScannerTest {

	@Test
	public void testIt() {
		Assertions.assertThat(DigitalOceanEntityScanner.toUrn("usw1", "aabbccddee", "droplet", "12340"))
				.isEqualTo("urn:digitalocean:cloud:usw1:aabbccddee:droplet/12340");
	}
}
