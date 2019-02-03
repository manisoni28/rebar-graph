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
import java.util.function.Supplier;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Config.ConfigBuilder;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;

class Neo4jDriverImpl extends GraphDriver {

	static ObjectMapper mapper = new ObjectMapper();


	Supplier<Driver> driverSupplier;
	

	public Neo4jDriverImpl(final Supplier<Driver> supplier) {
		com.google.common.base.Supplier<Driver> guavaSupplier = new com.google.common.base.Supplier<Driver>() {
			
			@Override
			public Driver get() {
				return supplier.get();
			}
		};
		
		this.driverSupplier = Suppliers.memoize(guavaSupplier);
	}
//	private Neo4jDriverImpl(Driver driver) {
//		this.driverSupplier = Suppliers.ofInstance(driver);
	
//	}

	

	private static boolean isNullOrEmpty(String in) {
		return in == null || in.isEmpty();
	}

	public Driver getDriver() {
		return driverSupplier.get();
	}

	public CypherTemplate cypher(String cypher) {
		return newTemplate().cypher(cypher);
	}

	public CypherTemplate newTemplate() {
		return new Neo4jTemplateImpl(this);
	}

	public GraphSchema schema() {
		return new Neo4jSchemaImpl(this);
	}
}
