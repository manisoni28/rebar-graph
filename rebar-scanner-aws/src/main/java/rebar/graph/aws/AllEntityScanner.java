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
package rebar.graph.aws;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class AllEntityScanner extends SerialScanner {

	

	public static List<Class<? extends AbstractEntityScanner>> getEntityScanners() {
		List<Class<? extends AbstractEntityScanner>> classList = Lists.newArrayList();
		classList.add(AccountScanner.class);
		classList.add(RegionScanner.class);
		classList.add(VpcScanner.class);
		classList.add(AvailabilityZoneScanner.class);
		classList.add(SecurityGroupScanner.class);
		classList.add(SubnetScanner.class);
		classList.add(Ec2InstanceScanner.class);
		classList.add(AmiScanner.class);
		classList.add(LaunchConfigScanner.class);
		classList.add(LaunchTemplateScanner.class);
		classList.add(ElbClassicScanner.class);
		classList.add(ElbScanner.class);
		classList.add(ElbTargetGroupScanner.class);
		
		classList.add(AsgScanner.class);
		classList.add(EksClusterScanner.class);
		classList.add(LambdaFunctionScanner.class);
		
		classList.add(RdsClusterScanner.class);
		classList.add(RdsInstanceScanner.class);
		return classList;
	}
	public AllEntityScanner() {
		super();


		
		addScanners(getEntityScanners());
	}

}
