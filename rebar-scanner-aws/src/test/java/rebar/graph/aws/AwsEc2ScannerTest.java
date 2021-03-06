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
package rebar.graph.aws;

import org.junit.jupiter.api.Test;

import com.amazonaws.regions.Regions;

public class AwsEc2ScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {
		deleteAllAwsEntities();
	}
	
	@Test
	public void testIt() {

		
		getAwsScanner().getEntityScanner(Ec2ScannerGroup.class);
		//getAwsScanner().newAsgScanner().scanLaunchConfigByName("foo");
		// getAwsScanner().newEc2Scanner().scanLaunchTemplates();
		 
	

	}

}
