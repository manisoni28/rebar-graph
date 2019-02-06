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
package rebar.graph.catalog;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import rebar.graph.core.RebarGraph;
import rebar.graph.core.Scanner;
import rebar.util.EnvConfig;

public class CatalogScanner extends Scanner {

	@Override
	protected void init(RebarGraph g, Map<String, String> config) {
	
			EnvConfig cfg = g.getEnvConfig();
				
			
				
				File baseDir = new File(cfg.get("CATALOG_DIR").orElse("."));
				
				filesystemLoader = new FilesystemCatalogLoader().withFile(baseDir);
				gitLoader = new GitCatalogLoader();
				
			
		
			
	}

	org.slf4j.Logger logger = LoggerFactory.getLogger(CatalogScanner.class);

	private static final String EMPTY_REGION = "";
	private static final String EMPTY_ACCOUNT = "";
	String uuid;

	GitCatalogLoader gitLoader = null;
	FilesystemCatalogLoader filesystemLoader = null;
	
	

	public File getScanBaseDir() {
		return new File(".");
	}

	@Override
	protected void doScan() {

		Collection<CatalogEntry> entries = filesystemLoader.scan();
		entries.forEach(it -> {
			tryExecute(() -> project(it));
		});

		if (gitLoader != null) {
			entries = gitLoader.scan();
			entries.forEach(it -> {
				tryExecute(() -> project(it));
			});
		}
	}

	String toUrn(CatalogEntry e) {

		String x = e.getType().name().replace("CatalogEntry", "").toLowerCase();
		return String.format("urn:rebar:catalog:%s:%s:%s/%s", EMPTY_REGION, EMPTY_ACCOUNT, x, e.getName());
	}

	public ObjectNode toJson(CatalogEntry entry) {
		ObjectNode n = (ObjectNode) entry.getData();
		n.put("graphEntityType", entry.getType().name());
		n.put("graphEntityGroup", "catalog");
		return n;
	}

	private void project(CatalogEntry e) {

		JsonNode n = toJson(e);
		String urn = toUrn(e);
		logger.info("writing catalog entry: {}", urn);
		getGraphBuilder().nodes(e.getType().name()).id("urn", toUrn(e)).properties(n).merge();
	}

	@Override
	public void scan(String scannerType, String a, String b, String c, String id) {

	}



}
