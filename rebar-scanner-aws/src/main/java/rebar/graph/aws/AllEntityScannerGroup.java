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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class AllEntityScannerGroup extends SerialScanner {

	

	public static List<Class<? extends AwsEntityScanner>> getEntityScanners() {
		List<Class<? extends AwsEntityScanner>> classList = Lists.newArrayList();
		classList.add(VpcScannerGroup.class);
		
		
		classList.add(Ec2ScannerGroup.class);
		
		classList.add(S3Scanner.class);
		
		
		classList.add(EksClusterScanner.class);
		classList.add(LambdaFunctionScanner.class);
		
		// NetworkScannerGroup is not really used by any other objects
		classList.add(NetworkScannerGroup.class);
		classList.add(RdsScannerGroup.class);
	
		classList.add(ApiGatewayScannerGroup.class);
		classList.add(ElasticMapReduceScannerGroup.class);
		classList.add(SqsScanner.class);
		classList.add(SnsScanner.class);
		classList.add(IamScannerGroup.class);
		classList.add(Route53Scanner.class);
		
		
		
		
		return classList;
	}
	public AllEntityScannerGroup() {
		super();


		
		addScanners(getEntityScanners());
	}

}
