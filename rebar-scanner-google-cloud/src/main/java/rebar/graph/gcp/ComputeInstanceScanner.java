package rebar.graph.gcp;

import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.util.Strings;

import rebar.util.Json;

public class ComputeInstanceScanner extends GcpEntityScanner {

	static final int METADATA_MAXLEN = 256;

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

		n.path("labels").fields().forEachRemaining(entry -> {
			String labelName = "label_" + entry.getKey();
			n.put(labelName, entry.getValue().asText());
		});

		n.path("metadata").path("items").forEach(it -> {
			String key = it.path("key").asText();
			String val = it.path("value").asText();
			if (val.length() > METADATA_MAXLEN) {
				logger.warn("truncating metadata for key: {}" ,key);
			
				val = val.substring(0, METADATA_MAXLEN);
			}
			if (!Strings.isNullOrEmpty(key)) {
				n.put("metadata_" + key, val);
			}

		});

		n.remove("labels");
		n.remove("metadata");

		getScanner().getGraphBuilder().nodes(getEntityType().name()).idKey("urn").properties(n).merge();

	}





	private void gc(GcpEntityType type, ProjectZone pz, long ts) {
		String cypher = "match (a:" + type.name()
				+ " {account:{account},projectId:{projectId}}) where a.graphUpdateTs<{ts} return a";

		getScanner().getGraphDriver().cypher(cypher).param("account", getAccount())
				.param("projectId", pz.getProjectId()).param("ts", ts).forEach(it -> {
					scan(it);
				});
	}

	@Override
	protected void doScan() {

		long ts = getScanner().getGraphBuilder().getTimestamp();
		getProjectZoneList().forEach(it -> {
			scanProject(it.getProjectId(), it.getZoneName());
			gc(getEntityType(), it, ts);
		});
		
		mergeRelatinships();
	}

	private void mergeRelatinships() {
		getScanner().getGraphBuilder().nodes(getEntityType().name()).relationship("RESIDES_IN").on("zone", "selfLink")
				.to(GcpEntityType.GcpZone.name()).merge();
		getScanner().getGraphBuilder().nodes(GcpEntityType.GcpProject.name()).relationship("HAS")
				.on("projectId", "projectId").to(GcpEntityType.GcpComputeInstance.name()).merge();

	}

	public void scanProject(String projectId, String zone) {

		logger.info("Scanning {} {}", projectId, zone);

		JsonNode n = getScanner().request().url("https://www.googleapis.com").path("compute").path("v1").path("projects").path(projectId).path("zones")
				.path(zone).path("instances").exec();
				

		n.path("items").forEach(it -> {
			tryExecute(() -> {
				project(it);
			});
		});

	}

}
