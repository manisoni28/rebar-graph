package rebar.graph.github;
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

import rebar.graph.test.AbstractIntegrationTest;

public class GitHubIntegrationTest extends AbstractIntegrationTest {

	Logger logger = LoggerFactory.getLogger(GitHubIntegrationTest.class);
	
	@Test
	public void testIt() throws Exception {
	
		
		getRebarGraph().newScanner(GitHubScanner.class).scanRepo("if6was9/minecraft");
	
		
		
	}
}
