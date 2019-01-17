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

class Neo4jDriverImpl extends GraphDriver {

	static ObjectMapper mapper = new ObjectMapper();
	Driver driver;

	Neo4jDriverImpl(Driver driver) {
		this.driver = driver;
	}

	

	private static boolean isNullOrEmpty(String in) {
		return in == null || in.isEmpty();
	}

	public Driver getDriver() {
		return driver;
	}

	public CypherTemplate cypher(String cypher) {
		return newTemplate().cypher(cypher);
	}

	public CypherTemplate newTemplate() {
		return new Neo4jTemplateImpl(driver);
	}

	public GraphSchema schema() {
		return new Neo4jSchemaImpl(this);
	}
}
