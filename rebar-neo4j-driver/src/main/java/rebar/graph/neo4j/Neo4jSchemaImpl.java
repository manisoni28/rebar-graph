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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4jSchemaImpl implements GraphSchema {

	Logger logger = LoggerFactory.getLogger(Neo4jSchemaImpl.class);
	Neo4jDriverImpl driver;

	private static final boolean APPLY_BY_DEFAULT=true;
	Neo4jSchemaImpl(Neo4jDriverImpl driver) {
		this.driver = driver;
	}

	public void createUniqueConstraint(String label, String attribute) {
		createUniqueConstraint(label, attribute,APPLY_BY_DEFAULT);
	}
	@Override
	public void createUniqueConstraint(String label, String attribute, boolean apply) {

		long count = driver.cypher("CALL db.indexes()").stream()
				.filter(n -> n.path("type").asText().equals("node_unique_property"))
				.filter(n -> n.path("tokenNames").path(0).asText().equals(label))
				.filter(n -> n.path("properties").path(0).asText().equals(attribute)).count();

		if (count == 0) {
			String cypher = "CREATE CONSTRAINT ON (x:" + label + ") ASSERT x."
					+ CypherUtil.escapePropertyName(attribute) + " IS UNIQUE";
			
			if (apply) {
				logger.info("{}",cypher);
				driver.cypher(cypher).exec();
			}
			else {
				logger.info("constraint *NOT* modified: {}",cypher);
			}
		} else {
			logger.info("unique constraint already exists for {}.{}", label, attribute);
		}

	}

	@Override
	public void dropUniqueConstraint(String label, String attribute) {
		
		dropUniqueConstraint(label, attribute, APPLY_BY_DEFAULT);
	}
	@Override
	public void dropUniqueConstraint(String label, String attribute, boolean apply) {
		// DROP CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE
		CypherUtil.assertValidLabel(label);

		long count = driver.cypher("CALL db.indexes()").stream()
				.filter(n -> n.path("type").asText().equals("node_unique_property"))
				.filter(n -> n.path("tokenNames").path(0).asText().equals(label))
				.filter(n -> n.path("properties").path(0).asText().equals(attribute)).count();

		if (count>0) {
			String cypher = String.format("DROP CONSTRAINT ON (a:%s) ASSERT a.%s IS UNIQUE", label,
					CypherUtil.escapePropertyName(attribute));
			
			if (apply) {
				logger.info("{}",cypher);
				driver.cypher(cypher).exec();
			}
			else {
				logger.info("constraint *NOT* modified: {}",cypher);
			}
		}
		else {
			logger.info("unique constraint does not exist on {}.{}",label,(attribute));
		}
	}

}
