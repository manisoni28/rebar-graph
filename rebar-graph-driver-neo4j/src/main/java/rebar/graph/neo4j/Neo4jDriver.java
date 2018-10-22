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
package rebar.graph.neo4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Config.ConfigBuilder;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import com.fasterxml.jackson.databind.ObjectMapper;

import rebar.graph.driver.GraphDriver;

public class Neo4jDriver extends GraphDriver {

	static ObjectMapper mapper = new ObjectMapper();
	Driver driver;

	Neo4jDriver(Driver driver) {
		this.driver = driver;
	}

	public static class Builder extends GraphDriver.Builder {
		Driver bDriver;

		List<Consumer<ConfigBuilder>> configConsumers = new LinkedList<>();
		
		@Override
		public Optional<String> getUrl() {
			return getEnv("NEO4J_URL");
		}

		@Override
		public Optional<String> getUsername() {
			return getEnv("NEO4J_USERNAME");
		}

		@Override
		public Optional<String> getPassword() {
			return getEnv("NEO4J_PASSWORD");
		}

		public Builder withDriver(Driver d) {
			this.bDriver = d;
			return this;
		}

	
		@Override
		public Optional<String> getEnv(String s) {
			Optional<String> val = super.getEnv(s);
			if (val.isPresent()) {
				return val;
			}
			
			s = s.replace("GRAPH_", "NEO4J_");
			val = super.getEnv(s.replace("GRAPH_","NEO4J_"));
			if (val.isPresent()) {
				return val;
			}
			
			val = super.getEnv(s.replace("NEO4J_","GRAPH_"));
			return val;
		}

		@Override
		public Builder withUrl(String url) {
			return (Builder) super.withUrl(url);
		}

		@Override
		public Builder withUsername(String username) {
			return (Builder) super.withUsername(username);
		}
		public Builder withCredentials(String username, String password) {
			return withUsername(username).withPassword(password);
		}
		@Override
		public Builder withPassword(String password) {
			return (Builder) super.withPassword(password);
		}

		@Override
		public Builder withEnv(String key, String val) {
			return (Builder) super.withEnv(key, val);
		}

		@Override
		public Builder withEnv(Map<String, String> env) {
			return (Builder) super.withEnv(env);
		}

		public Builder withConfig(Consumer<ConfigBuilder> configConsumer) {
			configConsumers.add(configConsumer);
			return (Builder) this;
		}
		private Config.ConfigBuilder applyOptions(Config.ConfigBuilder b) {
			
			
			
			getEnv("NEO4J_CONNECTION_LIVENESS_CHECK_TIMEOUT").ifPresent(it->{
				b.withConnectionLivenessCheckTimeout(Long.parseLong(it), TimeUnit.MILLISECONDS);
			});
			getEnv("NEO4J_CONNECTION_TIMEOUT").ifPresent(it->{
				b.withConnectionTimeout(Long.parseLong(it), TimeUnit.MILLISECONDS);
			});
			getEnv("NEO4J_ENCRYPTION_ENABLED").ifPresent(it->{
				if (!Boolean.parseBoolean(it)) {
					b.withoutEncryption();
				}
				else {
					b.withEncryption();
				}
				
			});
			getEnv("NEO4J_LEAKED_SESSION_LOGGING_ENABLED").ifPresent(it->{
				if (Boolean.parseBoolean(it)) {
					b.withLeakedSessionsLogging();
				}
				
				
			});
			
			/* This stuff uses neo4j-java-driver 1.5+
			getEnv("NEO4J_CONNECTION_ACQUISITION_TIMEOUT").ifPresent(it->{
				b.withConnectionAcquisitionTimeout(Long.parseLong(it), TimeUnit.MILLISECONDS);
			});
			getEnv("NEO4j_LOAD_BALANCING_STRATEGY").ifPresent(it->{
				b.withLoadBalancingStrategy(LoadBalancingStrategy.valueOf(it));
			});
			getEnv("NEO4J_MAX_CONNECTION_LIFETIME").ifPresent(it->{
				b.withMaxConnectionLifetime(Long.parseLong(it), TimeUnit.MILLISECONDS);
			});
			getEnv("NEO4J_MAX_CONNECTION_POOL_SIZE").ifPresent(it->{
				b.withMaxConnectionPoolSize(Integer.parseInt(it));
			});
			*/
			
			getEnv("NEO4J_MAX_TRANSACTION_RETRY_TIME").ifPresent(it->{
				b.withMaxTransactionRetryTime(Long.parseLong(it), TimeUnit.MILLISECONDS);
			});
		
			for (Consumer<ConfigBuilder> config: configConsumers) {
				config.accept(b);
			}
			return b;
		}
		public Neo4jDriver build() {
			if (bDriver != null) {
				return new Neo4jDriver(this.bDriver);
			} else {
				String url = getUrl().orElse("bolt://localhost:7687");
				

			
				Config config = applyOptions(Config.build()).toConfig();
				
				if ((getUsername().isPresent() && getPassword().isPresent())) {

					return new Neo4jDriver(GraphDatabase.driver(url,
							AuthTokens.basic(getUsername().get(), getPassword().get()), config));
				} else {
					return new Neo4jDriver(GraphDatabase.driver(url, config));
				}
			}
		}
	}

	private static boolean isNullOrEmpty(String in) {
		return in == null || in.isEmpty();
	}

	public Driver getDriver() {
		return driver;
	}

	public Neo4jTemplate cypher(String cypher) {
		return newTemplate().cypher(cypher);
	}

	public Neo4jTemplate newTemplate() {
		return new Neo4jTemplate(driver);
	}

}
