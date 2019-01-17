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
