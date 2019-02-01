package rebar.graph.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import rebar.util.Json;

public abstract class EntityScanner<SCANNER extends Scanner,ENTITYTYPE,OBJECTTYPE,CLIENT> {

	private Logger logger = LoggerFactory.getLogger(EntityScanner.class);
	private SCANNER scanner;
	
	
	protected void setScanner(SCANNER scanner) {
		Preconditions.checkArgument(this.scanner==null);
		this.scanner = scanner;
	}

	
	public final SCANNER getScanner() {
		return scanner;
	}

	protected abstract CLIENT getClient();
	public abstract ENTITYTYPE getEntityType();
	
	protected abstract void project(OBJECTTYPE t);
	
	protected ObjectNode toJson(OBJECTTYPE x) {
		ObjectNode n = Json.objectMapper().valueToTree(x);
		
		n.put("graphEntityType", getEntityType().toString());
	
		n.put("graphEntityGroup", getScanner().getEntityGroup());
		return n;
	}
	
	public void tryExecute(rebar.graph.core.Invokable x) {
		try {
			x.invoke();
		} catch (Exception e) {
			maybeThrow(e);
		}
	}
	
	public void maybeThrow(Exception e) {
		scanner.maybeThrow(e);
	}
	
	protected abstract void doScan();
	
	public void scan() {
		logger.info("scanning");
		doScan();
		logger.info("scan complete");
	}
}
