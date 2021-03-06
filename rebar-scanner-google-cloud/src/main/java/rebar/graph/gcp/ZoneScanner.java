package rebar.graph.gcp;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import rebar.util.Json;

public class ZoneScanner extends GcpEntityScanner {

	protected ZoneScanner(GcpScanner scanner) {
		super(scanner);

	}

	@Override
	public GcpEntityType getEntityType() {
		return GcpEntityType.GcpZone;
	}

	@Override
	protected void project(ObjectNode t) {

		String regionName = t.path("region").asText();
		int idx = regionName.lastIndexOf("/");
		regionName = regionName.substring(idx + 1);
		t.put("regionName", regionName);
		getScanner().getGraphBuilder().nodes(getEntityType().name()).idKey("urn").properties(t).merge();

		{

			ObjectNode region = Json.objectNode();

			region.put("graphEntityType", GcpEntityType.GcpRegion.name());
			region.put("graphEntityGroup", "gcp");
			region.put("regionName", regionName);
			region.put("region", t.path("region").asText());
			region.put("urn", "urn:gcp:compute:" + regionName);

			getScanner().getGraphBuilder().nodes(GcpEntityType.GcpRegion.name()).idKey("urn").properties(region).merge();
		}
	}

	@Override
	protected void doScan() {

		getProjectIds().forEach(projectId -> {
			JsonNode n = getScanner().request().url("https://www.googleapis.com").path("/compute/v1/projects/")
					.path(projectId).path("zones").exec();

			n.path("items").forEach(it -> {
				tryExecute(() -> project(toJson(it)));
			});
		});

		mergeRelationships();
	}

	private void mergeRelationships() {
		getScanner().getGraphBuilder().nodes(GcpEntityType.GcpZone.name()).relationship("RESIDES_IN").on("region", "region")
				.to(GcpEntityType.GcpRegion.name()).merge();

	}

	@Override
	public Optional<String> toUrn(ObjectNode t) {
		return toUrn(t.get("selfLink").asText());

	}

}
