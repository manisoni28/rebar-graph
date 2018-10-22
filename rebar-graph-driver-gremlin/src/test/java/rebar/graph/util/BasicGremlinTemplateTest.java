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
package rebar.graph.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public class BasicGremlinTemplateTest {

	static ObjectMapper mapper = new ObjectMapper();

	static Logger logger = LoggerFactory.getLogger(BasicGremlinTemplateTest.class);
	
	public static Map<Object,Object> mapOf(Object...args) {
		Map<Object,Object> val = new HashMap<>();
		for (int i=0; args!=null && i<args.length; i+=2) {
			val.put(args[i], args[i+1]);
		}
		return val;
	}
	@Test
	public void testIt() {
		TinkerGraph tg = TinkerFactory.createModern();

		
		tg.addVertex("Person").property("firstName", "Jerry").element().property("lastName", "Garcia").element().property("foo",mapOf("1","2")).element()
				.addEdge("PLAYS", tg.addVertex("Instrument").property("name", "guitar").element());

		tg.addVertex("Person").property("firstName", "Phil").element().property("lastName", "Lesh").element()
				.addEdge("PLAYS", tg.traversal().V().has("name", "guitar").tryNext().get());
		
		tg.traversal().V().hasLabel("Person").tryNext().get().property("fizz",mapOf("a","b","c","d")).element().property("buzz", mapOf("a","1")).element();
		
		

	}

}
