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
package rebar.graph.alibaba;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.sts.model.v20150401.GetCallerIdentityRequest;
import com.aliyuncs.sts.model.v20150401.GetCallerIdentityResponse;
import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.core.Scanner;
import rebar.graph.core.ScannerBuilder;
import rebar.util.Json;
import rebar.util.RebarException;

public class AlibabaScanner extends Scanner {

	DefaultProfile profile;

	public AlibabaScanner(ScannerBuilder<? extends Scanner> builder) {
		super(builder);
	
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
		
		vpcrs.getVpcs().forEach(it->{
			Json.logger().info(Json.objectMapper().valueToTree(it));
		});
		
		DescribeInstancesResponse response = (DescribeInstancesResponse) client.getAcsResponse(request);
		
		response.getInstances().forEach(it->{
			System.out.println(it);
			JsonNode n = Json.objectMapper().valueToTree(it);
			Json.logger().info(n);
		});
		
		}
		catch (com.aliyuncs.exceptions.ClientException e) {
			throw new RebarException(e);
		}
		
		
	}
	
	
	


}
