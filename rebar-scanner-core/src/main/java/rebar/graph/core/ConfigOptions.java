package rebar.graph.core;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import rebar.util.EnvConfig;

public class ConfigOptions {
	

	EnvConfig env;
	
	public ConfigOptions(EnvConfig cfg) {
		this.env = cfg;
	}
	
	public Optional<String> getDatabaseUrl() {
		return env.get("GRAPH_URL");
	}
	public Optional<String> getDatabaseUsername() {
		return env.get("GRAPH_USERNAME");
	}
	public Optional<String> getDatabasePassword() {
		return env.get("GRAPH_PASSWORD");
	}
	public boolean isIndexAutoCreateEnabled() {
		return Boolean.parseBoolean(env.get("INDEX_AUTOCREATE").orElse("true"));
	}
}
