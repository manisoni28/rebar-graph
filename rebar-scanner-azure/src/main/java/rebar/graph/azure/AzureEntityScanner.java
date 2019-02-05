package rebar.graph.azure;

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
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Subscription;

import rebar.graph.core.EntityScanner;
import rebar.util.Json;

public abstract class AzureEntityScanner<OBJECTTYPE> extends EntityScanner<AzureScanner, AzureEntityType, OBJECTTYPE, Azure> {

	Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

	@Override
	protected Azure getClient() {
		return getScanner().getAzureClient();
	}

	

	@Override
	public Optional<String> toUrn(OBJECTTYPE t) {

	
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
	protected void deleteByUrn(AzureEntityType type, String urn) {
		String cypher = "match (a:" + type.name() + " {urn:{urn}}) detach delete a";
		getScanner().getGraphDB().getNeo4jDriver().cypher(cypher).cypher(cypher).param("urn", urn).exec();
	}
	protected void deleteBySelfLink(AzureEntityType type, String url) {
		String cypher = "match (a:" + type.name() + " {selfLink:{selfLink}}) detach delete a";
		getScanner().getGraphDB().getNeo4jDriver().cypher(cypher).cypher(cypher).param("selfLink", url).exec();
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
	protected ObjectNode toJson(OBJECTTYPE x) {

		ObjectNode n = Json.objectMapper().valueToTree(x);
		n.put("subscriptionId", getSubscriptionId());
		n.put("graphEntityType", getEntityType().name());
		n.put("graphEntityGroup", "azure");

		return n;
	}

	



	public String getSubscriptionId() {
		return getClient().subscriptionId();
	}


	public final void project(JsonNode n) {
		project((ObjectNode) n);
	}

	public boolean isRegionEnabled(String regionName) {
		return Strings.nullToEmpty(regionName).startsWith("us-");
	}



	Optional<String> toUrn(String url) {

		return Optional.empty();
	}

	protected AzureEntityScanner(AzureScanner scanner) {
		setScanner(scanner);
	}

	

}
