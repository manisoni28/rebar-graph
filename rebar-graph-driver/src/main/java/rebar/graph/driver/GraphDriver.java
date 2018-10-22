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
package rebar.graph.driver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public abstract class GraphDriver {

	
	public static class Builder {
		
		Map<String,String> env = new HashMap<>();
		public Optional<String> getEnv(String s) {
			if (env.containsKey(s)) {
				return Optional.ofNullable(env.get(s));
			}
			String val = System.getProperty(s.toLowerCase().replace("_", "."));
			if (val!=null) {
				return Optional.ofNullable(val);
			}
			return Optional.ofNullable(System.getenv(s));
		}
		public Optional<String> getUrl() {
			return getEnv("GRAPH_URL");
		}
		public Builder withUrl(String url) {
			return withEnv("GRAPH_URL",url);
		}

		public Optional<String> getUsername() {
			return getEnv("GRAPH_USERNAME");
		}
		public Builder withUsername(String username) {
			return withEnv("GRAPH_USERNAME",username);
		}
		public Optional<String> getPassword() {
			return getEnv("GRAPH_PASSWORD");
		}
		public Builder withPassword(String password) {
			return withEnv("GRAPH_PASSWORD",password);
		}
		
		public Builder withEnv(String key, String val) {
			env.put(key, val);
			return this;
		}
		
		public Builder withEnv(Map<String,String> env) {
			this.env = new HashMap<>(env);
			return this;
		}
		public GraphDriver build()  {
			try {
				Optional url = getEnv("GRAPH_URL");
				if (url.isPresent()) {
				
					Builder b = (Builder) Class.forName("rebar.graph.neo4j.Neo4jDriver$Builder").newInstance();
					b.withEnv(env);
					return b.build();
				
				}
				else {
					throw new GraphException("GRAPH_URL not set");
				}
			}
			catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
				throw new GraphException(e);
			}
		}
	}
	public abstract GraphTemplate newTemplate();

}
