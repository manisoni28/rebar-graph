package rebar.graph.docker;
import org.junit.jupiter.api.Disabled;
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
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ImageInfo;

import rebar.util.Json;

public class TestMe {

	@Test
	@Disabled
	public void testIt() throws Exception {
		
		
		final DockerClient docker = DefaultDockerClient.fromEnv().build();
		
		docker.pull("rebar/rebar-scanner-kubernetes:latest");
		
		ImageInfo ii = docker.inspectImage("rebar/rebar-scanner-kubernetes:latest");
		
		
		JsonNode n = Json.objectMapper().valueToTree(ii);
		Json.logger().info(n);
		
		docker.listContainers().iterator().forEachRemaining(it->{
			JsonNode nx = Json.objectMapper().valueToTree(it);
			Json.logger().info(nx);
		});
	}
}
