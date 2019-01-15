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
package rebar.graph.kubernetes;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import rebar.graph.core.GraphDB;
import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.Neo4jDriver;

public class MergeServiceEndpointsOperation implements GraphOperation {

	@Override
	public Stream<JsonNode> exec(Scanner scanner, JsonNode n,Neo4jDriver d) {
		GraphDB g = scanner.getRebarGraph().getGraphDB();

		long ts = scanner.getRebarGraph().getGraphDB().getTimestamp();

		
		JsonNode podRef = n.path("podRef");
		// This is N+1 inefficient. Could use some optimization.
		List<String> podList = Lists.newArrayList();
		for (int i = 0; i < podRef.size(); i++) {
			String uid = podRef.get(i).asText();

			String clusterId = n.path("clusterId").asText();
			String endpointsUid = n.path("uid").asText();

			scanner.getRebarGraph().getGraphDB().nodes("KubeService").id("clusterId", clusterId)
					.id("namespace", n.path("namespace").asText()).id("name", n.path("name").asText())
					.relationship("EXPOSES").to("KubePod").id("uid", uid).merge();

		}

		d.cypher(
				"match (a:KubeService {uid:{uid},clusterId:{clusterId}})-[r]-(b:KubePod) where not (b.uid in {podList}) delete r")
				.param("clusterId", n.path("clusterId").asText()).param("uid", n.path("uid").asText())
				.param("podList", podList).exec();

		return Stream.of();
	}

}
