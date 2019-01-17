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

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;
import com.aliyuncs.auth.BasicCredentials;
import com.aliyuncs.auth.StaticCredentialsProvider;
import com.aliyuncs.profile.DefaultProfile;

import rebar.graph.core.ScannerBuilder;
import rebar.util.EnvConfig;

public class AlibabaScannerBuilder extends ScannerBuilder<AlibabaScanner> {



	AlibabaCloudCredentialsProvider credentialsProvider = null;
	String profileName;
	
	
	
	public AlibabaScannerBuilder withProfile(String profile) {
		this.profileName = profile;
		return this;
	}
	
	@Override
	public AlibabaScanner build() {
	
		EnvConfig cfg = new EnvConfig();
		
		if (cfg.get(AliyunConfig.ALIYUN_ACCESS_KEY_ID).isPresent() && cfg.get(AliyunConfig.ALIYUN_SECRET_ACCESS_KEY).isPresent()) {
			
			StaticCredentialsProvider scp = new StaticCredentialsProvider(new BasicCredentials(cfg.get(AliyunConfig.ALIYUN_ACCESS_KEY_ID).get(),
					cfg.get(AliyunConfig.ALIYUN_SECRET_ACCESS_KEY).get()));
			
			DefaultProfile dp = DefaultProfile.getProfile(cfg.get(AliyunConfig.ALIYUN_REGION).orElse("us-west-1"));
			dp.setCredentialsProvider(scp);
			AlibabaScanner scanner = new AlibabaScanner(this);
			scanner.profile = dp;
			return scanner;
		}
		
		
		DefaultProfile profile = null;
		if (profileName==null) {
			profile = AliyunConfig.load().getProfile();
		}
		else {
			profile = AliyunConfig.load().getProfile(profileName);
;		}
		
		AlibabaScanner scanner = new AlibabaScanner(this);
		scanner.profile = profile;
		return scanner;
	}

}
