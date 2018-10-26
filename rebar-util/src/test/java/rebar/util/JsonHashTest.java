/**
 * Copyright 2018 Rob Schoening
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

import rebar.util.Json;
import rebar.util.JsonHash;

public class JsonHashTest {

	@Test
	public void testIt() {

		Assertions.assertThat(JsonHash.sha256().digest(Json.objectNode().put("a", 1).put("b", 2)).hash().toString())
				.isEqualTo(JsonHash.sha256().digest(Json.objectNode().put("b", 2).put("a", 1)).hash().toString());

	}

}
