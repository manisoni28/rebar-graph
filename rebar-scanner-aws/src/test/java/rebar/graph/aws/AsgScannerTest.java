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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import rebar.util.Json;

public class AsgScannerTest extends AwsIntegrationTest {


	@Test
	public void testIT() {
		getAwsScanner().getEntityScanner(AccountScanner.class);
		getAwsScanner().getEntityScanner(VpcScanner.class);
		getAwsScanner().getEntityScanner(SubnetScanner.class);
		getAwsScanner().getEntityScanner(Ec2InstanceScanner.class).scan();
		getAwsScanner().getEntityScanner(AsgScanner.class).scan();
		
		
		// Verify that there are not any rogue relationships
		getRebarGraph().getGraphDB().getNeo4jDriver().cypher("match (a:AwsAsg)-[r]-(e:AwsEc2Instance) return a,r,e").forEach(it->{
		
			String instanceId = it.path("e").path("instanceId").asText();	
			Assertions.assertThat(it.path("a").path("instances").iterator()).anyMatch(p->p.asText().equals(instanceId));	
			assertSameAccountRegion(it.path("a"),it.path("e"));
		
		});
		
		
	}
	
	

}
