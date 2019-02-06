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
package rebar.graph.kubernetes;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import rebar.graph.core.GraphBuilder;
import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.GraphDriver;
import rebar.graph.neo4j.GraphException;
import rebar.util.Json;

public class RemoveStalePodContainersOperation implements GraphOperation {

	@SuppressWarnings("unchecked")
	@Override
	public Stream<JsonNode> exec(Scanner ctx, JsonNode n, GraphDriver neo4j) {

		try {
			String clusterId = n.path("clusterId").asText();
			String podUid = n.path("podUid").asText();
			ArrayNode an = (ArrayNode) n.path("containers");
			long ts = n.path(GraphBuilder.UPDATE_TS).asLong();
		
			List<String> list = Json.objectMapper().treeToValue(an, List.class);

			
			neo4j.cypher("match (x:KubeContainer {clusterId:{clusterId},podUid:{podUid}}) where (not x.containerID in {containers}) or (x.graphUpdateTs<{ts})  detach delete x")
					.param("clusterId", clusterId).param("podUid", podUid).param("containers",list).param("ts",ts).exec();;

			neo4j.cypher("match (p:KubePod {uid:{podUid},clusterId:{clusterId}})-[r]-(x:KubeContainer) where r.graphUpdateTs<{ts} detach delete r,x")
			.param("clusterId", clusterId).param("podUid", podUid).param("containers", list).param("ts",ts).exec();
			
			return Stream.of();
		} catch (JsonProcessingException e) {
			throw new GraphException(e);
		}
	}

	

}
