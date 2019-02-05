package rebar.graph.core;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import rebar.util.Json;

public abstract class EntityScanner<SCANNER extends Scanner,ENTITYTYPE,OBJECTTYPE,CLIENT> {

	private Logger logger = LoggerFactory.getLogger(getClass());
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
		ObjectNode n = Json.objectNode(); // do not use mapper, it doesn't work
		
		n.put("graphEntityType", getEntityType().toString());
		n.put("graphEntityGroup", getScanner().getEntityGroup());
		
		toUrn(x).ifPresent(urn->{
			n.put("urn",urn);
		});
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
	
	public abstract Optional<String> toUrn(OBJECTTYPE t);
}
