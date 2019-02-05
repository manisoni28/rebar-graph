package rebar.graph.docker;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListContainersFilterParam;
import com.spotify.docker.client.DockerClient.ListContainersParam;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.AttachedNetwork;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.shaded.com.google.common.base.Suppliers;

import rebar.util.Json;
import rebar.util.RebarException;

public class ContainerScanner extends DockerEntityScanner<Container> {

	@Override
	protected ObjectNode toJson(Container x) {

		ObjectNode n = super.toJson(x);

		n.put("hostId", getHostId());
		n.put("createTs", x.created());
		n.put("id", x.id());
		n.put("image", x.image());
		n.put("imageId", x.imageId());
		n.put("ports", x.portsAsString());
		n.put("state", x.state());
		n.put("status", x.status());
		n.put("command", x.command());
		ArrayNode names = n.arrayNode();
		x.names().forEach(it -> {
			names.add(it);
		});
		n.set("names", names);

		x.labels().forEach((k, v) -> {
			n.put("label_" + k, v);
		});

		x.mounts().forEach(it -> {

		});
		n.put("bridge", x.networkSettings().bridge());
		n.put("endpointId", x.networkSettings().endpointId());
		n.put("gateway", x.networkSettings().gateway());
		n.put("globalIPv6Address", x.networkSettings().globalIPv6Address());
		n.put("globalIPv6PrefixLen", x.networkSettings().globalIPv6PrefixLen());
		n.put("hairpinMode", x.networkSettings().hairpinMode());
		n.put("ipAddress", x.networkSettings().ipAddress());
		n.put("ipPrefixLen", x.networkSettings().ipPrefixLen());
		n.put("macAddress", x.networkSettings().macAddress());

		n.put("ipv6Gateway", x.networkSettings().ipv6Gateway());
		n.put("sandboxId", x.networkSettings().sandboxId());
		n.put("sandboxKey", x.networkSettings().sandboxKey());

		Map<String, Map<String, String>> portMapping = x.networkSettings().portMapping();

		if (portMapping != null) {

		}
		ArrayNode networkNames = n.arrayNode();
		n.set("networkNames", networkNames);
		Map<String, AttachedNetwork> networks = x.networkSettings().networks();

		networks.entrySet().forEach(it -> {
			networkNames.add(it.getKey());
			setNetwork(n, it.getKey(), "endpointId", it.getValue().endpointId());
			setNetwork(n, it.getKey(), "gateway", it.getValue().gateway());
			setNetwork(n, it.getKey(), "endpointId", it.getValue().globalIPv6Address());
			setNetwork(n, it.getKey(), "ipAddress", it.getValue().ipAddress());
			setNetwork(n, it.getKey(), "ipv6Gateway", it.getValue().ipv6Gateway());
			setNetwork(n, it.getKey(), "macAddress", it.getValue().macAddress());
			setNetwork(n, it.getKey(), "networkId", it.getValue().networkId());
			setNetwork(n, it.getKey(), "globalIpv6PrefixLen", it.getValue().globalIPv6PrefixLen());
			setNetwork(n, it.getKey(), "ipPrefixLen", it.getValue().ipPrefixLen());

		});
		return n;
	}

	private void setNetwork(ObjectNode n, String networkName, String attribute, String val) {
		n.put("network_" + networkName + "_" + attribute, val);
	}

	private void setNetwork(ObjectNode n, String networkName, String attribute, Integer val) {
		n.put("network_" + networkName + "_" + attribute, val);
	}

	ContainerScanner(DockerScanner scanner) {
		super(scanner);

	}

	@Override
	public DockerEntityType getEntityType() {
		return DockerEntityType.DockerContainer;
	}

	@Override
	protected void project(Container t) {
		ObjectNode n = toJson(t);
		getScanner().getRebarGraph().getGraphDB().nodes(DockerEntityType.DockerContainer.name()).idKey("urn")
				.properties(n).withTagPrefixes(TAG_PREFIXES).merge();

	}

	@Override
	protected void doScan() {

		long ts = getScanner().getGraphDB().getTimestamp();
		try {

			getClient().listContainers().forEach(it -> {
				tryExecute(() -> project(it));
			});
		} catch (DockerException | InterruptedException e) {
			throw new RebarException(e);
		}

		gc(DockerEntityType.DockerContainer, ts);
		mergeRelationships();
	}

	private void mergeRelationships() {
		getScanner().getRebarGraph().getGraphDB().nodes(DockerEntityType.DockerHost.name()).id("id", getHostId())
				.relationship("HAS").on("id", "hostId").to(DockerEntityType.DockerContainer.name()).merge();

	}

	@Override
	public Optional<String> toUrn(Container t) {
		return Optional.ofNullable(String.format("urn:docker:%s:container/%s", getHostId().replace(":", "-"), t.id()));
	}

	private void deleteContainerId(String id) {
		getScanner().getGraphDB().nodes(DockerEntityType.DockerContainer.name()).id("id", "id")
				.id("hostId", getHostId()).delete();
	}

	protected void scanContainerId(String id) {
		try {
			Optional<Container> c = getScanner().getDockerClient()
					.listContainers(ListContainersFilterParam.allContainers(true)).stream()
					.filter(p -> p.id().equals(id)).findFirst();
			if (!c.isPresent()) {
				deleteContainerId(id);
			}
			
		} catch (DockerException | InterruptedException e) {
			throw new RebarException(e);
		}

	}

	@Override
	void scan(JsonNode n) {
		String id = n.path("id").asText();
		scanContainerId(id);

	}

}
