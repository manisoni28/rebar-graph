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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class EnvConfig {

	private Map<String,String> env = ImmutableMap.of();
	
	public EnvConfig() {
		
	}

	public EnvConfig withEnv(String key, String val) {
		
		Map<String,String> copy = new HashMap<>(env);
		copy.put(key, val);
		
		EnvConfig cfg = new EnvConfig();
		cfg.env = ImmutableMap.copyOf(copy);
		return cfg;
	}
	public Optional<String> get(String key) {
		
		if (key == null) {
			return Optional.empty();
		}
		key = key.trim();
		String val = env.get(key);
		if (val!=null &&  (!val.trim().isEmpty())) {
			return Optional.ofNullable(val);
		}
		
		val = System.getenv(key);
		if (val!=null &&  (!val.trim().isEmpty())) {
			return Optional.ofNullable(val);
		}
		String lowerKey = key.toLowerCase().replace("_", ".");
		
		val = System.getProperty(lowerKey);
		if (val!=null && (!val.trim().isEmpty())) {
			return Optional.ofNullable(val);
		}
		return Optional.empty();
	}
}
