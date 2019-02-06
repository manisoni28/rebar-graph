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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.myjeeva.digitalocean.exception.DigitalOceanException;
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException;
import com.myjeeva.digitalocean.impl.DigitalOceanClient;

import rebar.graph.core.GraphBuilder.NodeOperation;
import rebar.graph.core.RebarGraph;
import rebar.graph.core.Scanner;
import rebar.util.RebarException;

public class DigitalOceanScanner extends Scanner {

	org.slf4j.Logger logger = LoggerFactory.getLogger(DigitalOceanScanner.class);
	DigitalOceanClient client;

	String uuid;

	public DigitalOceanScanner() {
		

	}

	protected synchronized DigitalOceanClient getDigitalOceanClient() {
		if (client == null) {
			Optional<String> token = getEnvConfig().get("DIGITALOCEAN_ACCESS_TOKEN");
			File configFile = new File(System.getProperty("user.home"), ".config/doctl/config.yaml");
			if (!token.isPresent()) {
				try {

					if (configFile.exists()) {
						logger.info("loading config from {}", configFile);
						JsonNode n = new ObjectMapper(new YAMLFactory()).readTree(configFile);
						String t = n.path("access-token").asText(null);
						token = Optional.ofNullable(t);
					}

				} catch (IOException e) {
					throw new RebarException(e);
				}
			}

			if (!token.isPresent()) {
				logger.info("you must either set DIGITALOCEAN_ACCESS_TOKEN or place config in {}", configFile);
				logger.info("TIP: use doctl https://github.com/digitalocean/doctl");
				throw new RebarException("could not obtain token");
			}

			client = new DigitalOceanClient("v2", token.get());
		}
		return client;
	}

	synchronized String getAccount() {
		try {
			if (uuid == null) {
				uuid = getDigitalOceanClient().getAccountInfo().getUuid();
			}
			return uuid;
		} catch (DigitalOceanException | RequestUnsuccessfulException e) {
			throw new RebarException(e);
		}
	}

	@Override
	protected void doScan() {

		getAccountScanner().scan();
		getRegionScanner().scan();
		getDropletScanner().scan();

	}

	public DigitalOceanDropletScanner getDropletScanner() {
		return new DigitalOceanDropletScanner(this);
	}

	protected NodeOperation digitalOceanNodes(String label) {

		return getGraphBuilder().nodes(label).id("account", getAccount());

	}

	public DigitalOceanRegionScanner getRegionScanner() {
		return new DigitalOceanRegionScanner(this);
	}

	public DigitalOceanAccountScanner getAccountScanner() {
		return new DigitalOceanAccountScanner(this);
	}

	@Override
	public void scan(String scannerType, String a, String b, String c, String id) {
		// TODO Auto-generated method stub

	}


	@Override
	protected void init(RebarGraph g, Map<String, String> config) throws Exception {
	
		
	}

}
