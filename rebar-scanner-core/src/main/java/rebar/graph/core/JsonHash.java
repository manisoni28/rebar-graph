package rebar.graph.core;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class JsonHash {

	HashFunction hashFunction = Hashing.sha256();
	
	boolean isIgnored(String name) {
		if (name==null) {
			return true;
		}
		
		return false;
	}
	public String hash(JsonNode n) {
		
		Hasher hasher = hashFunction.newHasher();
		
		List<String> names = Lists.newArrayList(n.fieldNames());
		Collections.sort(names);
		names.forEach(it->{
			if (isIgnored(it)) {
				// 
			}
			else {
				hasher.putString(it, Charsets.UTF_8);
				JsonNode val = n.path(it);
				
				if (val.isObject()) {
					// ignore
				}
				else if (val.isArray()) {
					
				}
				else {
					hasher.putString(val.asText(),Charsets.UTF_8);
				}
			}
		});
		
		return hasher.hash().toString();
	}

}
