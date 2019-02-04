package rebar.graph.gcp;

import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import rebar.util.Json;

public class ComputeInstanceScanner extends GcpEntityScanner {

	protected ComputeInstanceScanner(GcpScanner scanner) {
		super(scanner);
		// TODO Auto-generated constructor stub
	}

	@Override
	public GcpEntityType getEntityType() {
		return GcpEntityType.GcpComputeInstance;
	}

	@Override
	protected void project(ObjectNode t) {
		ObjectNode n = toJson(t);

		getScanner().getGraphDB().nodes(getEntityType().name()).idKey("urn").properties(n).merge();

	}

	@Override
	protected void doScan() {

		getProjectZoneList().forEach(it -> {
			scanProject(it.getProjectId(), it.getZoneName());
		});
		mergeRelatinships();
	}

	private void mergeRelatinships() {
		getScanner().getGraphDB().nodes(getEntityType().name()).relationship("RESIDES_IN").on("zone", "selfLink")
				.to(GcpEntityType.GcpZone.name()).merge();
		getScanner().getGraphDB().nodes(GcpEntityType.GcpProject.name()).relationship("HAS").on("projectId", "projectId")
		.to(GcpEntityType.GcpComputeInstance.name()).merge();
		
	}

	public void scanProject(String projectId, String zone) {

		logger.info("Scanning {} {}", projectId, zone);

		JsonNode n = getScanner().get("https://www.googleapis.com",
				String.format("/compute/v1/projects/%s/zones/%s/instances", projectId, zone));

		n.path("items").forEach(it -> {
			tryExecute(() -> {
				project(it);
			});
		});

	}

}
