package rebar.graph.digitalocean;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.reflect.Invokable;
import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import com.myjeeva.digitalocean.pojo.RateLimit;

import rebar.graph.core.EntityScanner;
import rebar.graph.core.GraphDB.NodeOperation;
import rebar.graph.neo4j.GraphDriver;
import rebar.util.Json;

public abstract class DigitalOceanEntityScanner<T>
		extends EntityScanner<DigitalOceanScanner, DigitalOceanEntityType, T, DigitalOceanClient> {

	Logger logger = LoggerFactory.getLogger(getClass());

	protected void adviseRateLimit(DigitalOceanEntityType type, RateLimit limit) {
		logger.info("{} limit={} remaining={} reset={}sec", type, limit.getLimit(), limit.getRemaining(),
				limit.getReset().toString());
		
			
	}

	protected DigitalOceanEntityScanner(DigitalOceanScanner scanner) {
		setScanner(scanner);
	}

	public String getAccount() {
		return getScanner().getAccount();
	}

	public NodeOperation digitalOceanNodes(String label) {
		return getScanner().digitalOceanNodes(label);
	}

	public DigitalOceanClient getClient() {
		return getScanner().getDigitalOceanClient();
	}

	protected abstract void project(T entity);

	protected ObjectNode toJson(T entity) {
		ObjectNode n = super.toJson(entity);
		n.put("account", getAccount());
		if (n.has("id")) {
			n.put("id", n.path("id").asText());
		}
		n.put("urn", toUrn(entity).get());
		return n;
	}

	protected GraphDriver getGraphDriver() {
		return getScanner().getGraphDriver();
	}

	public abstract void scan(String id);

	public abstract void scan(JsonNode n);

	protected void gc(DigitalOceanEntityType t, long ts) {
		Stopwatch sw = Stopwatch.createStarted();
		AtomicInteger count = new AtomicInteger(0);
		getScanner().getRebarGraph().getGraphDB().getNeo4jDriver()
				.cypher("match (a:" + t.name() + " {account:{account}}) where a.graphUpdateTs<{ts} return a")
				.param("ts", ts).param("account", getAccount()).forEach(it -> {
					if (it.path("graphEntityType").asText().equals(getEntityType().toString())) {
						count.incrementAndGet();
						tryExecute(() -> scan(it));
					}
				});

		if (count.get() > 0) {
			logger.info("gc for {} count={} took {} ms", t, count.get(), sw.elapsed(TimeUnit.MILLISECONDS));
		}

	}

	public void deleteNode(String keyName, String id) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(keyName));
		Preconditions.checkArgument(!Strings.isNullOrEmpty(id));
		logger.info("deleting {}",MoreObjects.toStringHelper(getEntityType().name()).add("account", getAccount()).add("keyName", id).toString());
	
		getScanner().getGraphDB().nodes(getEntityType().name()).id(keyName, id).idKey("account").delete();

	}
	public static String toUrn(String region, String account, String resourceType, String resource) {
		return String.format("urn:digitalocean:cloud:%s:%s:%s/%s",region, account, resourceType, resource);
	}
	public abstract java.util.Optional<String> toUrn(T t);
}
