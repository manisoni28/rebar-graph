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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import rebar.util.Json;

public class JsonTest {


	
	@Test
	public void test() {
		
		Json.logger().info(null,Json.objectNode().put("a","b"));
	}
	
	
	@Test
	public void testPrettyPrint() {
		
		List<JsonNode> list = new ArrayList<>();
		list.add(Json.objectNode().put("foo", "bar"));
		
		
		list.stream().forEach(Json.logger(JsonTest.class)::info);
	}
	
	
	@Test
	public void testIt() {
		Json.logger().debug("debug message",Json.objectNode().put("ts", System.currentTimeMillis()));
		Json.logger().info("info message",Json.objectNode().put("ts", System.currentTimeMillis()));
		Json.logger().warn("warn message",Json.objectNode().put("ts", System.currentTimeMillis()));
		Json.logger().error("error message",Json.objectNode().put("ts", System.currentTimeMillis()));
		
		Json.logger().info("foo",null);
		
		JsonNode n = Json.objectNode().put("foo", "bar");
		Json.logger().info(n);
		Json.logger().debug(n);
		Json.logger().warn(n);
		Json.logger().error(n);
	}
	
	

}
