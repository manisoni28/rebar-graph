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

import rebar.graph.core.EntityScanner;
import rebar.graph.core.GraphDB.NodeOperation;
import rebar.util.Json;

public abstract class DigitalOceanEntityScanner<T> extends EntityScanner<DigitalOceanScanner,DigitalOceanEntityType,T,DigitalOceanClient>{

	Logger logger = LoggerFactory.getLogger(getClass());

	protected void adviseRateLimit(DigitalOceanEntityType type, RateLimit limit) {
		logger.info("{} limit={} remaining={} reset={}",type,limit.getLimit(),limit.getRemaining(),limit.getReset().toString());
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

		return n;
	}
}
