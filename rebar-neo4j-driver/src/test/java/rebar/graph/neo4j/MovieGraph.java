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
package rebar.graph.neo4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;

public class MovieGraph {

	Neo4jDriverImpl client;

	public MovieGraph(Neo4jDriverImpl c) {

		this.client = c;
	}

	public void deleteMovieGraph() {

		client.cypher("MATCH (p:Person) where exists (p.testData) detach delete p").exec();

		client.cypher("MATCH (m:Movie) where exists (m.testData) detach delete m").exec();

	}

	public void replaceMovieGraph() {

		try {

			deleteMovieGraph();

			executeClasspath("movies.cypher");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void executeClasspath(String name) throws IOException {

		URL url = Thread.currentThread().getContextClassLoader().getResource(name);

		InputStream is = url.openStream();

		BufferedReader sr = new BufferedReader(new InputStreamReader(is));
		String line = null;
		StringWriter sw = new StringWriter();
		while ((line = sr.readLine()) != null) {
			sw.write(line);
			sw.write("\n");
		}
		String val = sw.toString();

		client.cypher(val).exec();

	}
}
