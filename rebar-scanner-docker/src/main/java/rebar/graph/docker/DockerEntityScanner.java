package rebar.graph.docker;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.spotify.docker.client.DockerClient;

import rebar.graph.core.EntityScanner;

public abstract class DockerEntityScanner<OBJECTTYPE>
		extends EntityScanner<DockerScanner, DockerEntityType, OBJECTTYPE, DockerClient> {

	static Logger logger = LoggerFactory.getLogger(DockerEntityScanner.class);
	protected Set<String> TAG_PREFIXES = ImmutableSet.of("label_", "network_");

	DockerEntityScanner(DockerScanner scanner) {
		super();
		setScanner(scanner);
	}

	public String getHostId() {
		return getScanner().getHostId();
	}

	public final DockerClient getClient() {
		return getScanner().getDockerClient();
	}

	abstract void scan(JsonNode n);

	protected void gc(DockerEntityType type, long ts) {
		getScanner().getGraphBuilder().getNeo4jDriver()
				.cypher("match (a:" + type.name() + " {hostId:{hostId}}) where a.graphUpdateTs<{ts} return a")
				.param("ts", ts).param("hostId", getHostId()).forEach(it -> {
				
					scan(it);
				});
	}
}
