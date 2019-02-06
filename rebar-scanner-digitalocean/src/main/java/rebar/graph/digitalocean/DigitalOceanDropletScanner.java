package rebar.graph.digitalocean;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.myjeeva.digitalocean.exception.DigitalOceanException;
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException;
import com.myjeeva.digitalocean.pojo.Droplet;
import com.myjeeva.digitalocean.pojo.Droplets;

import rebar.util.Json;

public class DigitalOceanDropletScanner extends DigitalOceanEntityScanner<Droplet> {

	DigitalOceanDropletScanner(DigitalOceanScanner scanner) {

		super(scanner);

	}

	@Override
	protected void doScan() {

		tryExecute(() -> {

			long ts = getScanner().getGraphBuilder().getTimestamp();
			Droplets result = null;
			boolean done = false;
			final int PAGE_SIZE = 100;
			for (int i = 0; i < 100 && !done; i++) {

				result = getClient().getAvailableDroplets(i, PAGE_SIZE);
				adviseRateLimit(getEntityType(), result.getRateLimit());
				done = result.getDroplets().size() < PAGE_SIZE;
				result.getDroplets().forEach(it -> {
					tryExecute(() -> project(it));
				});
			}
			gc(DigitalOceanEntityType.DigitalOceanDroplet, ts);

		});

		mergeRelationships("DigitalOceanDroplet");
	}

	protected void mergeRelationships(String label) {
		digitalOceanNodes("DigitalOceanAccount").relationship("HAS").on("account", "account").to("DigitalOceanDroplet")
				.merge();
		digitalOceanNodes("DigitalOceanDroplet").relationship("RESIDES_IN").on("region", "region")
				.to("DigitalOceanRegion").merge();
	}

	@Override
	protected void project(Droplet entity) {
		ObjectNode n = toJson(entity);

		digitalOceanNodes("DigitalOceanDroplet").idKey("urn").properties(n).merge();
	}

	@Override
	protected ObjectNode toJson(Droplet entity) {

		ObjectNode n = super.toJson(entity);

		n.set("imageId", n.path("image").path("id"));
		n.set("imageName", n.path("image").path("name"));
		n.path("networks").path("version4Networks").forEach(it -> {
			String type = it.path("type").asText();
			if (type.equals("public")) {
				n.set("publicIpAddress", it.path("ipAddress"));
				n.set("publicNetmask", it.path("netmask"));
				n.set("publicGateway", it.path("gateway"));
			} else if (type.equals("private")) {
				n.set("privateIpAddress", it.path("ipAddress"));
				n.set("privateNetmask", it.path("netmask"));
				n.set("privateGateway", it.path("gateway"));
			}
		});
		n.set("region", n.path("region").path("slug"));
		n.remove("networks");
		n.remove("image");

		return n;
	}

	@Override
	public DigitalOceanEntityType getEntityType() {
		return DigitalOceanEntityType.DigitalOceanDroplet;
	}

	@Override
	public void scan(String id) {
		try {
			Droplet d = getClient().getDropletInfo(Integer.parseInt(id));
			project(d);
		} catch (DigitalOceanException e) {
			if (e.getMessage().contains("not be found")) {
				deleteNode("id", id);
				return;
			}
			maybeThrow(e);
		} catch (RequestUnsuccessfulException e) {

			maybeThrow(e);
		}
	}

	@Override
	public void scan(JsonNode n) {
		String id = n.path("id").asText();

		scan(id);

	}

	@Override
	public Optional<String> toUrn(Droplet t) {
		return Optional.ofNullable(toUrn(t.getRegion().getSlug(), getAccount(), "droplet", Integer.toString(t.getId())));
	
	}

}
