/**
 * Copyright 2018-2019 Rob Schoening
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
package rebar.graph.alibaba;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.aliyuncs.auth.AlibabaCloudCredentials;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;
import com.aliyuncs.auth.BasicCredentials;
import com.aliyuncs.auth.StaticCredentialsProvider;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;

import rebar.util.RebarException;

public class AliyunConfig {

	public static final String ALIYUN_SECRET_ACCESS_KEY="ALIYUN_SECRET_ACCESS_KEY";
	public static final String ALIYUN_ACCESS_KEY_ID="ALIYUN_ACCESS_KEY_ID";
	public static final String ALIYUN_REGION="ALIYUN_REGION";
	
	JsonNode json;
	
	public JsonNode getProfileConfig() {
		return getProfileConfig(json.path("current").asText());
	}
	public JsonNode getProfileConfig(String contextName) {
	
		for (JsonNode profile: json.path("profiles")) {
			
			if (profile.path("name").asText().equals(contextName)) {
				return profile;
			}
		}
		throw new RebarException("profile not found: '"+contextName+"'");
	}
	
	
	public DefaultProfile getProfile(String name) {
		JsonNode context = null;
		
		if (name==null) {
			context = getProfileConfig();
		}
		else {
			 context = getProfileConfig(name);
		}
		String regionId = context.path("region_id").asText();
		DefaultProfile p = DefaultProfile.getProfile(regionId);
		p.setCredentialsProvider(getCredentialsProvider());
		return p;
	}
	public DefaultProfile getProfile() {
		JsonNode context = getProfileConfig();
		String regionId = context.path("region_id").asText();
		DefaultProfile p = DefaultProfile.getProfile(regionId);
		p.setCredentialsProvider(getCredentialsProvider());
		return p;
		
	}
	public AlibabaCloudCredentialsProvider getCredentialsProvider() {
		
		AlibabaCloudCredentialsProvider p = new AlibabaCloudCredentialsProvider() {
			
			@Override
			public AlibabaCloudCredentials getCredentials() throws ClientException, ServerException {
				
				
				JsonNode profile = getProfileConfig();
				String accessKeyId = profile.path("access_key_id").asText();
				String accessKeySecret = profile.path("access_key_secret").asText();
				return new BasicCredentials(accessKeyId,accessKeySecret);
				
			
			}
		};
		
		return p;
		
	}
	public static AliyunConfig load() {
	
		String userHome = System.getProperty("user.home");
		File f = new File(userHome,".aliyun/config.json");
		return load(f);
	}
	public static AliyunConfig load(File f) {
	
		try (FileReader fr = new FileReader(f)) {
			AliyunConfig cfg = new AliyunConfig();
			cfg.json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(fr);
			return cfg;
		}
		catch (IOException e) {
			throw new RebarException(e);
		}
		

	}

}
