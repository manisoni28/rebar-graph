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

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.Neo4jDriver;

public class RemoveStaleRelationshipsOperation implements GraphOperation {

	@Override
	public Stream<JsonNode> exec(Scanner scanner, JsonNode n,Neo4jDriver driver) {
		
		
		String ownerType = n.path("ownerType").asText();
		if (Strings.isNullOrEmpty(ownerType)) {
			return Stream.of();
		}
		String ownerProperty = KubeScanner.toOwnerRefProperty(ownerType);

		
		String cypher = "match (a:"+n.path("ownerType").asText()+")-[r]-(b:"+n.path("childType").asText()+") where not exists (b."+ownerProperty+" ) delete r";
	
		driver.cypher(cypher).exec();
		
		return Stream.of();
	}



	

}
