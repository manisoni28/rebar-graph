package rebar.graph.docker;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Info;
import com.spotify.docker.client.shaded.com.fasterxml.jackson.databind.node.ObjectNode;

import rebar.util.Json;
import rebar.util.Json.JsonLogger;

public class HostScanner extends DockerEntityScanner<Info> {



	HostScanner(DockerScanner scanner) {
		super(scanner);

	}

	@Override
	public DockerEntityType getEntityType() {
		return DockerEntityType.DockerHost;
	}

	@Override
	protected void project(Info t) {
		com.fasterxml.jackson.databind.node.ObjectNode n = toJson(t);
		
		
		getScanner().getGraphBuilder().nodes(getEntityType().name()).idKey("urn").properties(n).merge();

	}

	@Override
	protected void doScan() {

		tryExecute(() -> {
			Info info = getClient().info();

			project(info);
		});
		
	}

	@Override
	public Optional<String> toUrn(Info t) {
		return Optional.ofNullable(String.format("urn:docker:%s", t.id().replace(":", "-")));
		
	}

	
	@Override
	protected com.fasterxml.jackson.databind.node.ObjectNode toJson(Info x) {
	
		com.fasterxml.jackson.databind.node.ObjectNode n =  super.toJson(x);
		n.put("architecture", x.architecture());
		n.put("cgroupDriver", x.cgroupDriver());
		n.put("clusterStore", x.clusterStore());
		n.put("containers", x.containers());
		n.put("containersPaused", x.containersPaused());
		n.put("containersRunning", x.containersRunning());
		n.put("containersStopped", x.containersStopped());
		n.put("cpuCfsPeriod", x.cpuCfsPeriod());
		n.put("cpuCfsQuota", x.cpuCfsQuota());
		n.put("cpus", x.cpus());
		n.put("debug", x.debug());
		n.put("dockerRootDir", x.dockerRootDir());
		n.put("eventsListener", x.eventsListener());
		n.put("experimentalBuild", x.experimentalBuild());
		n.put("fileDescriptors", x.fileDescriptors());
		n.put("httpProxy", x.httpProxy());
		n.put("httpsProxy", x.httpsProxy());
		n.put("noProxy", x.noProxy());
		n.put("id", x.id());
		n.put("goroutines", x.goroutines());
		n.put("kernelMemory", x.kernelMemory());
		n.put("kernelVersion", x.kernelVersion());
		n.put("name", x.name());
		n.put("operatingSystem", x.operatingSystem());
		n.put("osType", x.osType());
		n.put("serverVersion", x.serverVersion());
		n.put("storageDriver", x.storageDriver());
		n.set("labels", Json.objectMapper().valueToTree(x.labels()));
		return n;
	}

	@Override
	void scan(JsonNode n) {
		// do nothing
		
	}

}
