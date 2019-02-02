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

import rebar.graph.core.ScannerBuilder;
import rebar.util.EnvConfig;

public class CatalogScannerBuilder extends ScannerBuilder<CatalogScanner> {



	
	
	
	@Override
	public CatalogScanner build() {
	
		EnvConfig cfg = getRebarGraph().getEnvConfig();
		
		CatalogScanner scanner = new CatalogScanner(this);
		
		File baseDir = new File(cfg.get("CATALOG_DIR").orElse("."));
		
		scanner.filesystemLoader = new FilesystemCatalogLoader().withFile(baseDir);
		scanner.gitLoader = new GitCatalogLoader();
		
	
		return scanner;
	}

}
