/**
 * Copyright 2018 Rob Schoening
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rebar.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.base.Charsets;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class JsonHash {

	Hasher hasher;
	HashFunction hashFunction;

	Set<String> ignoredAttributes = ImmutableSet.of();

	private static final byte NULL_TYPE = 0;
	private static final byte ARRAY_TYPE = 1;
	private static final byte OBJECT_TYPE = 2;
	private static final byte VALUE_TYPE = 3;

	public static JsonHash sha256() {
		JsonHash h = new JsonHash();
		h.hashFunction = Hashing.sha256();
		h.hasher = h.hashFunction.newHasher();
		return h;
	}


	static void hash(Hasher hasher, JsonNode n, Predicate<String> ignore) {
		if (n == null || n instanceof NullNode || n instanceof MissingNode) {
			hasher.putByte(NULL_TYPE);
			hasher.putBytes("null".getBytes(Charsets.UTF_8));
		} else if (n.isArray()) {
			hasher.putByte(ARRAY_TYPE);
			n.forEach(it -> {

				hash(hasher, it, ignore);
			});
		} else if (n.isObject()) {
			hasher.putByte(OBJECT_TYPE);
			List<String> names = Lists.newArrayList(n.fieldNames());
			Collections.sort(names);
			names.forEach(it -> {
				if (ignore != null && ignore.test(it)) {

				} else {
					hasher.putBytes(it.getBytes(Charsets.UTF_8));
					hash(hasher, n.get(it), ignore);
				}
			});
		} else if (n.isValueNode()) {
			hasher.putByte(VALUE_TYPE);
			hasher.putBytes(n.asText().toString().getBytes(Charsets.UTF_8));
		} else {
			System.out.println("WTF: " + n);
		}
	}

	public JsonHash digest(JsonNode n) {
		Predicate<String> p = new Predicate<String>() {

			@Override
			public boolean test(String t) {
				return ignoredAttributes.contains(t);
			}
			
		};
		hash(this.hasher, n,p);
		return this;
	}

	public HashCode hash() {
		return this.hasher.hash();
	}
}
