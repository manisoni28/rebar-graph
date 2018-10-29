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
