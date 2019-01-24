package rebar.graph.core;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * Computes a digest of a given json object.  This is intended to be used for simple change detection, for cache optimization.  At the moment, we don't 
 * really care that the hash is canonical...just that it is consistent.
 * @author rob
 *
 */
public class JsonDigest {

	List<Pattern> excludePatterns = Lists.newCopyOnWriteArrayList();
	
	boolean isExcluded(String name) {
		if (name==null) {
			return true;
		}
		for (Pattern p: excludePatterns) {
			if (p.matcher(name).matches()) {
				return true;
			}
		}
		return false;
	}
	
	public void digest(Hasher hasher, JsonNode n) {
	
		if (n==null) {
			hasher.putInt(JsonNodeType.NULL.ordinal());
			return;
		}
		if (n.isValueNode()) {
			hasher.putInt(n.getNodeType().ordinal());
			hasher.putString(n.asText(),Charsets.UTF_8);
			return;
		}
		else if (n.isArray()) {
			n.forEach(x->{
				digest(hasher,x);
			});
			return;
		}
		else if (n.isObject()) {
			List<String> names = Lists.newArrayList(n.fieldNames());
			Collections.sort(names);
			hasher.putInt(n.getNodeType().ordinal());
			names.forEach(it->{
				if (isExcluded(it)) {
					// do nothing
				}
				else {
				
					hasher.putString(it,Charsets.UTF_8);
					digest(hasher,n.path(it));
				}
				
			});
		}
		else {
			
			hasher.putInt(n.getNodeType().ordinal());	
		}
	}
	public String digest(JsonNode n) {
		
		HashFunction hashFunction = Hashing.sha256();
		Hasher hasher = hashFunction.newHasher();
		digest(hasher,n);
		

		
		return hasher.hash().toString();
	}
	
	public JsonDigest excludePattern(String regex) {
		excludePatterns.add(Pattern.compile(regex));
		return this;
	}

}
