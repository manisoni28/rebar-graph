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
package rebar.graph.neo4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Config.ConfigBuilder;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;

import rebar.util.EnvConfig;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

public abstract class GraphDriver {

	public static final String GRAPH_URL = "GRAPH_URL";
	public static final String GRAPH_USERNAME = "GRAPH_USERNAME";
	public static final String GRAPH_PASSWORD = "GRAPH_PASSWORD";

	CypherMetrics cypherMetrics;

	Supplier<Driver> driverSupplier;

	public static class Builder {

		EnvConfig env = new EnvConfig();
		Driver bDriver;

		List<Consumer<ConfigBuilder>> configConsumers = new LinkedList<>();

		MeterRegistry builderMeterRegistry = Metrics.globalRegistry; // default

		private Config.ConfigBuilder applyOptions(Config.ConfigBuilder b) {

			getEnv("NEO4J_CONNECTION_LIVENESS_CHECK_TIMEOUT").ifPresent(it -> {
				b.withConnectionLivenessCheckTimeout(Long.parseLong(it), TimeUnit.MILLISECONDS);
			});
			getEnv("NEO4J_CONNECTION_TIMEOUT").ifPresent(it -> {
				b.withConnectionTimeout(Long.parseLong(it), TimeUnit.MILLISECONDS);
			});
			getEnv("NEO4J_ENCRYPTION_ENABLED").ifPresent(it -> {
				if (!Boolean.parseBoolean(it)) {
					b.withoutEncryption();
				} else {
					b.withEncryption();
				}

			});
			getEnv("NEO4J_LEAKED_SESSION_LOGGING_ENABLED").ifPresent(it -> {
				if (Boolean.parseBoolean(it)) {
					b.withLeakedSessionsLogging();
				}

			});

			getEnv("NEO4J_MAX_TRANSACTION_RETRY_TIME").ifPresent(it -> {
				b.withMaxTransactionRetryTime(Long.parseLong(it), TimeUnit.MILLISECONDS);
			});

			for (Consumer<ConfigBuilder> config : configConsumers) {
				config.accept(b);
			}

			return b;
		}

		public Builder withEnv(EnvConfig e) {
			this.env = e;
			return this;
		}

		public Optional<String> getEnv(String s) {
			return env.get(s);
		}

		public Optional<String> getUrl() {
			return getEnv(GRAPH_URL);
		}

		public Builder withUrl(String url) {
			return withEnv(GRAPH_URL, url);
		}

		public Builder withDriver(Driver d) {
			this.bDriver = d;
			return this;
		}

		public Optional<String> getUsername() {
			return getEnv(GRAPH_USERNAME);
		}

		public Builder withUsername(String username) {
			return withEnv(GRAPH_USERNAME, username);
		}

		public Optional<String> getPassword() {
			return getEnv(GRAPH_PASSWORD);
		}

		public Builder withPassword(String password) {
			return withEnv(GRAPH_PASSWORD, password);
		}

		public Builder withMetricsRegistry(MeterRegistry reg) {
			builderMeterRegistry = reg;
			return this;
		}

		public Builder withEnv(String key, String val) {
			env = env.withEnv(key, val);
			return this;
		}

		public Builder withEnv(Map<String, String> env) {
			this.env = this.env.withEnv(env);
			return this;
		}

		public GraphDriver build() {
			try {
				Optional<String> url = getEnv(GRAPH_URL);

				Neo4jDriverImpl d = null;
				if (bDriver != null) {
	
					d = new Neo4jDriverImpl(Suppliers.ofInstance(bDriver));
				} else {

					if (!url.isPresent()) {
						url = Optional.ofNullable("bolt://localhost:7687");
					}

					Config config = applyOptions(Config.build()).toConfig();

					final String finalUrl = url.get();
					if ((getUsername().isPresent() && getPassword().isPresent())) {
					
						
						
						com.google.common.base.Supplier<Driver> xxx = (() -> {
							return GraphDatabase.driver(finalUrl,
									AuthTokens.basic(getUsername().get(), getPassword().get()), config);
						});
						d = new Neo4jDriverImpl(memoize(xxx));

					} else {
						com.google.common.base.Supplier<Driver> xx = (() -> {
							return GraphDatabase.driver(finalUrl, config);
						});
						
						d = new Neo4jDriverImpl(xx);
					}
				}
				Preconditions.checkNotNull(d);
				d.cypherMetrics = new CypherMetrics(d, builderMeterRegistry);

				return d;
			} catch (GraphException e) {
				throw e;
			} catch (RuntimeException e) {
				throw new GraphException(e);
			}
		}
	}

	static Supplier<Driver> memoize(Supplier<Driver> x) {
		com.google.common.base.Supplier<Driver> guavaSupplier = new com.google.common.base.Supplier<Driver>() {

			@Override
			public Driver get() {
				return x.get();
			}
		};
		return Suppliers.memoize(guavaSupplier);
	}

	public abstract CypherTemplate cypher(String cypher);

	public abstract CypherTemplate newTemplate();

	public abstract GraphSchema schema();

	public abstract Driver getDriver();

	public final CypherMetrics metrics() {
		return cypherMetrics;
	}
}
