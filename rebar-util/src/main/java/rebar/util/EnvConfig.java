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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class EnvConfig {

	private Map<String, String> env = Maps.newConcurrentMap();

	public EnvConfig() {

	}

	public EnvConfig copy() {
		EnvConfig copy = new EnvConfig();
		copy.env = new HashMap<>(copy.env);
		return copy;
	}

	public EnvConfig withEnv(Map<String, String> vals) {
		Preconditions.checkNotNull(vals);
		EnvConfig copy = copy();
		return copy;
	}

	public EnvConfig withEnv(String key, String val) {

		

		EnvConfig cfg = copy();
		cfg.env.put(key, val);
		return cfg;
	}

	public Optional<String> get(String key) {

		if (key == null) {
			return Optional.empty();
		}
		key = key.trim();
		String val = env.get(key);
		if (val != null && (!val.trim().isEmpty())) {
			return Optional.ofNullable(val);
		}

		val = System.getenv(key);
		if (val != null && (!val.trim().isEmpty())) {
			return Optional.ofNullable(val);
		}
		String lowerKey = key.toLowerCase().replace("_", ".");

		val = System.getProperty(lowerKey);
		if (val != null && (!val.trim().isEmpty())) {
			return Optional.ofNullable(val);
		}
		return Optional.empty();
	}
}
