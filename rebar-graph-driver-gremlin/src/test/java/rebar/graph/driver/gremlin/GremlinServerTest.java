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
package rebar.graph.driver.gremlin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import rebar.graph.driver.gremlin.GremlinDriver;

public abstract class GremlinServerTest {

	static Logger logger = LoggerFactory.getLogger(GremlinServerTest.class);
	static GremlinDriver gremlinDriver = null;

	static int attempts = 0;

	public GremlinDriver getGremlinDriver() {
		return gremlinDriver;
	}

	@BeforeEach
	public void setupGremlinServer() {

		if (gremlinDriver != null) {
			return;
		}
		if (attempts < 1) {
			try {
				attempts++;
				GremlinDriver gd = new GremlinDriver.Builder().build();
				gd.traversal().V().hasLabel("ConnectionCheck").limit(1).tryNext();
				gremlinDriver = gd;
			} catch (Exception e) {
				logger.warn("could not connect ... tests will be skipped - ", e);
			}
		}
		Assumptions.assumeTrue(gremlinDriver != null, "gremlin server not available");
	}

	

}
