package rebar.graph.core;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

/** 
 * Provide a simple way to trigger scans.
 * 
 * @author rob
 *
 */
public interface ScanQueue {

	public void submit(String type, String a, String b, String c, String d);
	
	public void subscribe(String type, String a, String b, Consumer<JsonNode> s);
	
}
