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

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;

import rebar.util.Sleep;

public class AwsScannerTest extends AwsIntegrationTest {

	@Test
	public void testIt() {
		
		
	
		Assertions.assertThat(getAwsScanner()).isSameAs(getAwsScanner());
		
		Assertions.assertThat((Object) getAwsScanner().getClient(AmazonEC2ClientBuilder.class)).isSameAs(getAwsScanner().getClient(AmazonEC2ClientBuilder.class));
		
	}
	
	@Test
	public void testX() {
		
		
		getAwsScanner().scan("aws", getAwsScanner().getAccount(), getAwsScanner().getRegion().getName(), "ami", "aa");
	}
	
	void assertScanner(String name, Class type) {
		AwsEntityScanner s = getAwsScanner().getEntityScannerForType(name);
		Assertions.assertThat(s.getClass()).isSameAs(type);
	}
	@Test
	public void testEntityScanners() {
		assertScanner("securitygroup",SecurityGroupScanner.class);
		assertScanner("vpc",VpcScanner.class);
		assertScanner("ami",AmiScanner.class);
		

	}

}
