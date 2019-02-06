package rebar.graph.gcp;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.graph.core.EntityScanner;
import rebar.util.Json;

public abstract class GcpEntityScanner extends EntityScanner<GcpScanner, GcpEntityType, ObjectNode, GcpScanner> {

	Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

	@Override
	protected GcpScanner getClient() {
		return getScanner();
	}

	protected ObjectNode toJson(JsonNode n) {
		return toJson((ObjectNode) n);
	}

	@Override
	public Optional<String> toUrn(ObjectNode t) {

	
		if (t.has("selfLink")) {
		
			return toUrn(t.path("selfLink").asText());
		}
		
		return Optional.empty();

	}
	
	protected void scan(JsonNode n) {

		String selfLink = n.path("selfLink").asText();

		try {
			JsonNode nx = getScanner().getUrl(selfLink );
			project(nx);

		} catch (ResourceNotFoundException e) {
			logger.info("deleting resource: {}",selfLink);
			deleteBySelfLink(getEntityType(), selfLink);
		}

	}
	protected void deleteByUrn(GcpEntityType type, String urn) {
		String cypher = "match (a:" + type.name() + " {urn:{urn}}) detach delete a";
		getScanner().getGraphBuilder().getNeo4jDriver().cypher(cypher).cypher(cypher).param("urn", urn).exec();
	}
	protected void deleteBySelfLink(GcpEntityType type, String url) {
		String cypher = "match (a:" + type.name() + " {selfLink:{selfLink}}) detach delete a";
		getScanner().getGraphBuilder().getNeo4jDriver().cypher(cypher).cypher(cypher).param("selfLink", url).exec();
	}
	protected Optional<String> extractProjectId(String url) {
		Pattern p = Pattern.compile(".*\\/projects\\/(.*?)\\/.*");
		Matcher m = p.matcher(Strings.nullToEmpty(url));
		if (m.matches()) {
			return Optional.of(m.group(1));
		}
		return Optional.empty();
	}
	@Override
	protected ObjectNode toJson(ObjectNode x) {

		String selfLink = x.path("selfLink").asText();
		
		ObjectNode n = x;

		extractProjectId(selfLink).ifPresent(id->{
			n.put("projectId", id);
		});
		toUrn(n).ifPresent(it -> {

			n.put("urn", it);
		});
		n.put("account", getAccount());
		n.put("graphEntityType", getEntityType().name());
		n.put("graphEntityGroup", "gcp");

		return n;
	}

	public List<ProjectZone> getProjectZoneList() {
		List<ProjectZone> list = Lists.newLinkedList();
		getProjectIds().forEach(project -> {
			getZones().forEach(zone -> {
				if (isRegionEnabled(zone)) {
					list.add(new ProjectZone(project, zone));
				}
			});
		});

		return list;
	}

	public List<String> getProjectIds() {
		return getScanner().getRebarGraph().getGraphBuilder().getNeo4jDriver()
				.cypher("match (a:GcpProject) return a.projectId as projectId").stream()
				.map(n -> n.path("projectId").asText()).collect(Collectors.toList());
	}

	public String getAccount() {
		return "000000000000";
	}
	public List<String> getZones() {
		return getScanner().getRebarGraph().getGraphBuilder().getNeo4jDriver()
				.cypher("match (a:GcpZone) return a.name as name").stream().map(n -> n.path("name").asText())
				.collect(Collectors.toList());
	}

	public final void project(JsonNode n) {
		project((ObjectNode) n);
	}

	public boolean isRegionEnabled(String regionName) {
		return Strings.nullToEmpty(regionName).startsWith("us-");
	}

	protected String zoneToRegion(String name) {
		int idx = name.lastIndexOf("-");
		String zoneCode = name.substring(idx + 1);
		if (zoneCode.length() == 1) {
			return name.substring(0, idx);
		}
		return name;
	}

	Optional<String> toUrn(String url) {

		Matcher m = GcpScanner.RESOUCE_EXTRACTOR.matcher(Strings.nullToEmpty(url));
		if (m.matches()) {

			String emptyProduct = getProduct();
		
			String emptyRegion = "";

			String urn = String.format("urn:gcp:%s:%s:%s:%s:%s", emptyProduct, emptyRegion, getAccount(), m.group(1),
					m.group(2));

			return Optional.ofNullable(urn);
		}
		return Optional.empty();
	}

	protected GcpEntityScanner(GcpScanner scanner) {
		setScanner(scanner);
	}

	public String getProduct() {
		switch (getEntityType()) {
		case GcpComputeImage:
		case GcpComputeInstance:
		case GcpZone:
			return "compute";
		case GcpProject:
			return "resource";
		}
		throw new IllegalStateException("cannot determine product: " + getEntityType());
	}

}
