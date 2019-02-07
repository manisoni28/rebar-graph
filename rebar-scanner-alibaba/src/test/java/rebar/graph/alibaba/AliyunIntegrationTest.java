package rebar.graph.alibaba;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyuncs.sts.model.v20150401.GetCallerIdentityRequest;
import com.aliyuncs.sts.model.v20150401.GetCallerIdentityResponse;
import com.google.common.base.Preconditions;

import rebar.graph.test.AbstractIntegrationTest;

public class AliyunIntegrationTest extends AbstractIntegrationTest {

	Logger logger = LoggerFactory.getLogger(AliyunIntegrationTest.class);
	
	static AliyunScanner scanner;
	static boolean skipAll=false;
	@Override
	protected void beforeAll() {
		
		
		super.beforeAll();
		
		
		getGraphDriver().cypher("match (a) where labels(a)[0]=~'Aliyun.*' detach delete a").exec();
		checkAccess();
		getScanner().scan();
	}

		
	@BeforeEach
	private void checkAccess() {
		try {
			if (scanner==null) {
				scanner = getRebarGraph().newScanner(AliyunScanner.class);
				GetCallerIdentityResponse response = scanner.getClient().getAcsResponse(new GetCallerIdentityRequest());

				skipAll=false;
			}
		
		}
		catch (Exception e) {
			logger.info("",e);
			skipAll = true;
		}
		Assumptions.assumeTrue(scanner!=null && (!skipAll));
	}
	public AliyunScanner getScanner() {
		Preconditions.checkState(scanner!=null,"scanner not initialized");
		return scanner;
	}
	
	
	@Test
	public void testIt() throws Exception {
	
		try {
		
		AliyunScanner scanner = getRebarGraph().newScanner(AliyunScanner.class);
	

		scanner.scan();
		
		}
		catch (Exception e) {
			logger.debug("ignore",e);
		}
		
		
		
		
	}
}
