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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class Json {

	static ObjectMapper mapper = new ObjectMapper();

	public static class JsonLogger {
		Logger logger;
		String message;
	
		JsonLogger(Logger logger, String message) {
			Preconditions.checkNotNull(logger);
			this.logger = logger;
			this.message = message;
		}
		public void info(JsonNode object) {
			info(message,object);
		}
		public void info(String msg, JsonNode object) {
			try {
				if (msg==null) {
					msg = message;
				}
				logger.info("{}\n{}", Strings.nullToEmpty(msg),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
			} catch (RuntimeException | JsonProcessingException e) {
				logger.warn("problem logging - {}",e.toString());
			}
		}
		
		public void debug(JsonNode object) {
			debug(message,object);
		}
		public void debug(String msg, JsonNode object) {
			try {
				if (msg==null) {
					msg = message;
				}
				logger.debug("{}\n{}", Strings.nullToEmpty(msg),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
			} catch (RuntimeException | JsonProcessingException e) {
				logger.warn("problem logging - {}",e.toString());
			}
		}
		
		public void warn(JsonNode object) {
			warn(message,object);
		}
		public void warn(String msg, JsonNode object) {
			try {
				if (msg==null) {
					msg = message;
				}
				logger.warn("{}\n{}", Strings.nullToEmpty(msg),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
			} catch (RuntimeException | JsonProcessingException e) {
				logger.warn("problem logging - {}",e.toString());
			}
		}
		
		public void error(JsonNode object) {
			error(message,object);
		}
		public void error(String msg, JsonNode object) {
			try {
				if (msg==null) {
					msg = message;
				}
				logger.error("{}\n{}", Strings.nullToEmpty(msg),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
			} catch (RuntimeException | JsonProcessingException e) {
				logger.warn("problem logging - {}",e.toString());
			}
		}
	}

	
	public static JsonLogger logger() {
		return logger((String)null);
	}
	/**
	 * WARNING: This is slow and inefficient!
	 * @return
	 */
	public static JsonLogger logger(String message) {
	

		for (StackTraceElement ste: Thread.currentThread().getStackTrace()) {
			
			String cn = ste.getClassName();
			if (!ImmutableList.of("java.lang.Thread",Json.class.getName()).contains(cn) && (!cn.contains("sun.reflect"))) {
				return logger(LoggerFactory.getLogger(cn),message);
			}
	
		}
		return logger(LoggerFactory.getLogger(Json.class),message);
	}
	public static JsonLogger logger(Logger logger) {
		return logger(logger);
	}
	public static JsonLogger logger(Logger logger, String message) {
		return new JsonLogger(logger,message);
	}
	public static JsonLogger logger(Class<?> clazz) {
		return logger(clazz,null);
	}
	public static JsonLogger logger(Class<?> clazz, String message) {
		return new JsonLogger(LoggerFactory.getLogger(clazz),message);
	}
	public static ObjectMapper objectMapper() {
		return mapper;
	}

	public static ObjectNode objectNode() {
		return mapper.createObjectNode();
	}

	public static ArrayNode arrayNode() {
		return mapper.createArrayNode();
	}


	public static List<JsonNode> toList(ArrayNode n) {
		
		List<JsonNode> list = Lists.newArrayList();
		
		n.forEach(it->{
			list.add(it);
		});
		return list;
	}


}
