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
package rebar.graph.doc;

import java.io.File;
import java.io.IOException;

import rebar.graph.neo4j.Neo4jDriver;

public class GraphDoc {

	
	
	public static void main(String [] args) throws IOException {
		DataModelMarkdown dm2 = new DataModelMarkdown().parse(new File("../rebar-scanner-aws/README.md")).withNeo4jDriver(new Neo4jDriver.Builder().build());
		dm2.mergeAll(p->p.startsWith("Aws")).write();
	
		
		dm2 = new DataModelMarkdown().parse(new File("../rebar-scanner-kubernetes/README.md")).withNeo4jDriver(new Neo4jDriver.Builder().build());
		dm2.mergeAll(p->p.startsWith("Kube")).write();
	
	}

}
