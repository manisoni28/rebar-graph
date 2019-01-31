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
package rebar.graph.digitalocean;


import com.myjeeva.digitalocean.impl.DigitalOceanClient;

import rebar.graph.core.ScannerBuilder;
import rebar.util.EnvConfig;

public class DigitalOceanScannerBuilder extends ScannerBuilder<DigitalOceanScanner> {



	
	
	
	@Override
	public DigitalOceanScanner build() {
	
		EnvConfig cfg = getRebarGraph().getEnvConfig();
		
		DigitalOceanScanner scanner = new DigitalOceanScanner(this);
		
		
	
		return scanner;
	}

}
