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
package rebar.graph.docker;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

import rebar.graph.core.RebarGraph;
import rebar.graph.core.Scanner;
import rebar.util.RebarException;

public class DockerScanner extends Scanner {

	DockerClient client;

	Supplier<String> hostIdSupplier;
	public DockerScanner() {
		hostIdSupplier = (Supplier<String>) Suppliers.memoizeWithExpiration(this::lookupHostId, 5,TimeUnit.MINUTES);
	}

	public void doScan() {
		hostScanner().scan();
		containerScanner().scan();
	}


	public String getHostId() {
		return hostIdSupplier.get();
	}
	private String lookupHostId() {
		try {
			String hostId = getDockerClient().info().id();
			return hostId;
		} catch (DockerException | InterruptedException e) {
			throw new RebarException(e);
		}
	}
	public DockerClient getDockerClient() {
		try {

			if (client == null) {
				DockerClient docker = DefaultDockerClient.fromEnv().build();
				this.client = docker;
			}
			return client;
		} catch (DockerCertificateException e) {
			throw new RebarException(e);
		}
	}

	@Override
	protected void init(RebarGraph g, Map<String, String> config) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void scan(String scannerType, String a, String b, String c, String d) {
		// TODO Auto-generated method stub

	}



	
	public ContainerScanner containerScanner() {
		return new ContainerScanner(this);
	}
	public HostScanner hostScanner() {
		return new HostScanner(this);
	}
}
