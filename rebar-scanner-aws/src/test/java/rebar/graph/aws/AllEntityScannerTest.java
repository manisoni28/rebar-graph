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

import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.machinezoo.noexception.Exceptions;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class AllEntityScannerTest extends AwsIntegrationTest {



	@Test
	public void testCompleteness() {
		AllEntityScannerGroup s = getAwsScanner().getEntityScanner(AllEntityScannerGroup.class);
		
		s.getScannerClasses().forEach(it->{
			logger.info("scanner: {}",it.getSimpleName());
		});
		
		Assertions.assertThat(s.getScannerClasses().get(0)).isEqualTo(VpcScannerGroup.class);
		Assertions.assertThat(s.getScannerClasses().get(1)).isEqualTo(Ec2ScannerGroup.class);
		
		
		Assertions.assertThat(s.getScannerClasses()).containsAll(ImmutableList.of(S3Scanner.class,EksClusterScanner.class,RdsScannerGroup.class,SqsScanner.class,SnsScanner.class,Route53Scanner.class));

	}

}
