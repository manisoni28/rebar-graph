package rebar.graph.digitalocean;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.reflect.Invokable;
import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import com.myjeeva.digitalocean.pojo.RateLimit;

import rebar.graph.core.GraphDB.NodeOperation;
import rebar.util.Json;

public abstract class DigitalOceanEntityScanner<T> {

	private DigitalOceanEntityType entityType;
	Logger logger = LoggerFactory.getLogger(getClass());
	private DigitalOceanScanner scanner;

	protected void adviseRateLimit(DigitalOceanEntityType type, RateLimit limit) {
		logger.info("{} limit={} remaining={} reset={}",type,limit.getLimit(),limit.getRemaining(),limit.getReset().toString());
	}
	DigitalOceanEntityScanner(DigitalOceanScanner scanner, DigitalOceanEntityType entityType) {
		this.scanner = scanner;
		this.entityType = entityType;
	}

	protected abstract void doScan();

	public String getAccount() {
		return scanner.getAccount();
	}

	public final void scan() {
		logger.info("scanning");
		doScan();
		logger.info("scan complete");
	}

	public NodeOperation digitalOceanNodes(String label) {
		return getDigitalOceanScanner().digitalOceanNodes(label);
	}

	public DigitalOceanScanner getDigitalOceanScanner() {
		return scanner;
	}

	public DigitalOceanClient getClient() {
		return scanner.getDigitalOceanClient();
	}

	public void maybeThrow(Exception e) {
		scanner.maybeThrow(e);
	}

	public void tryExecute(rebar.graph.core.Invokable x) {
		try {
			x.invoke();
		} catch (Exception e) {
			maybeThrow(e);
		}
	}

	protected abstract void project(T entity);

	
	protected final DigitalOceanEntityType getEntityType() {
		Preconditions.checkNotNull(entityType,"entityType not set");
		return entityType;
	}
	
	protected ObjectNode toJson(T entity) {
		ObjectNode n = Json.objectMapper().valueToTree(entity);
		n.put("account", getAccount());
		n.put("graphEntityType", getEntityType().name());
		n.put("graphEntityGroup", "digitalocean");
		return n;
	}
}
