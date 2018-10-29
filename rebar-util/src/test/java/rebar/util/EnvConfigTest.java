package rebar.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class EnvConfigTest {

	
	@Test
	public void testImmutability() {
		
		EnvConfig cfg = new EnvConfig();
		
		Assertions.assertThat(cfg).isNotSameAs(cfg.withEnv("FOO","BAR"));
		
		
		Assertions.assertThat(cfg.withEnv("PATH", "fizz").get("PATH").get()).isEqualTo("fizz");
		
	}
	@Test
	public void testIt() {
		EnvConfig c = new EnvConfig();

		if (System.getenv("PATH") != null) {

			Assertions.assertThat(c.get("PATH").get()).isEqualTo(System.getenv("PATH"));
		}
		if (System.getenv("USER") != null) {

			Assertions.assertThat(c.get("USER").get()).isEqualTo(System.getenv("USER"));
		}

			
		
		
	}

}
