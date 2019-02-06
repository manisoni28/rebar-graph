package rebar.graph.gcp;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import rebar.util.Json;

public class ProjectScanner extends GcpEntityScanner {

	protected ProjectScanner(GcpScanner scanner) {
		super(scanner);

	}

	@Override
	public GcpEntityType getEntityType() {
		return GcpEntityType.GcpProject;
	}

	@Override
	protected void project(ObjectNode t) {

		ObjectNode n = toJson(t);

		getScanner().getRebarGraph().getGraphBuilder().nodes(GcpEntityType.GcpProject.name()).idKey("urn").properties(n)
				.merge();

	}

	@Override
	protected void doScan() {
		JsonNode n = getScanner().request().url("https://cloudresourcemanager.googleapis.com").path("/v1/projects")
				.exec();
		n.path("projects").forEach(it -> {
			project(it);
		});
	}

	@Override
	public Optional<String> toUrn(ObjectNode t) {
		String product = "resource";
		String emptyRegion = ""; // intentionally empty
		String emptyAccount = "";
		return Optional.ofNullable(
				String.format("urn:gcp:%s:%s:%s:%s", product, emptyRegion, emptyAccount, t.path("projectId").asText()));

	}

}
