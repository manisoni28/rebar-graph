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

import java.util.Map;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.BasicCredentials;
import com.aliyuncs.auth.StaticCredentialsProvider;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.sts.model.v20150401.GetCallerIdentityRequest;
import com.aliyuncs.sts.model.v20150401.GetCallerIdentityResponse;
import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.core.RebarGraph;
import rebar.graph.core.Scanner;
import rebar.util.EnvConfig;
import rebar.util.Json;
import rebar.util.RebarException;

public class AlibabaScanner extends Scanner {

	String profileName=null;
	DefaultProfile profile;

	public void init(RebarGraph g, Map<String,String> m) {
		EnvConfig cfg = getEnvConfig();

		if (cfg.get(AliyunConfig.ALIYUN_ACCESS_KEY_ID).isPresent()
				&& cfg.get(AliyunConfig.ALIYUN_SECRET_ACCESS_KEY).isPresent()) {

			StaticCredentialsProvider scp = new StaticCredentialsProvider(
					new BasicCredentials(cfg.get(AliyunConfig.ALIYUN_ACCESS_KEY_ID).get(),
							cfg.get(AliyunConfig.ALIYUN_SECRET_ACCESS_KEY).get()));

			DefaultProfile dp = DefaultProfile.getProfile(cfg.get(AliyunConfig.ALIYUN_REGION).orElse("us-west-1"));
			dp.setCredentialsProvider(scp);

			profile = dp;
			return;
		}

		DefaultProfile profile = null;
		if (profileName == null) {
			profile = AliyunConfig.load().getProfile();
		} else {
			profile = AliyunConfig.load().getProfile(profileName);
			;
		}

		
	}

	@Override
	public void doScan() {
		try {
			IAcsClient client = new DefaultAcsClient(profile);

			DescribeInstancesRequest request = new DescribeInstancesRequest();
			request.setPageSize(10);
			request.setConnectTimeout(5000); // Set the connection timeout to 5000 milliseconds
			request.setReadTimeout(5000);

			DescribeVpcsRequest vpcr = new DescribeVpcsRequest();

			DescribeVpcsResponse vpcrs = client.getAcsResponse(vpcr);

			GetCallerIdentityRequest cirq = new GetCallerIdentityRequest();
			GetCallerIdentityResponse cirr = client.getAcsResponse(cirq);

			Json.logger().info(Json.objectMapper().valueToTree(cirr));

			vpcrs.getVpcs().forEach(it -> {
				Json.logger().info(Json.objectMapper().valueToTree(it));
			});

			DescribeInstancesResponse response = (DescribeInstancesResponse) client.getAcsResponse(request);

			response.getInstances().forEach(it -> {

				JsonNode n = Json.objectMapper().valueToTree(it);
				Json.logger().info(n);
			});

		} catch (com.aliyuncs.exceptions.ClientException e) {
			throw new RebarException(e);
		}

	}

	@Override
	public void scan(String scannerType, String a, String b, String c, String id) {
		// TODO Auto-generated method stub

	}



}
