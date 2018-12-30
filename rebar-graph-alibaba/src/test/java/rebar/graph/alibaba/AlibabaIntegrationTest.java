package rebar.graph.alibaba;
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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;
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

import rebar.graph.alibaba.AlibabaScanner;
import rebar.graph.alibaba.AlibabaScannerBuilder;
import rebar.graph.test.AbstractIntegrationTest;
import rebar.util.Json;

public class AlibabaIntegrationTest extends AbstractIntegrationTest {

	Logger logger = LoggerFactory.getLogger(AlibabaIntegrationTest.class);
	
	@Test
	public void testIt() throws Exception {
	
		try {
		
		AlibabaScanner scanner = getRebarGraph().createBuilder(AlibabaScannerBuilder.class).build();
	

		scanner.scan();
		
		}
		catch (Exception e) {
			logger.debug("ignore",e);
		}
		
		
		
		
	}
}
