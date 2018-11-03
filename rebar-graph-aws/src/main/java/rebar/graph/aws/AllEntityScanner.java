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

public class AllEntityScanner extends SerialScanner {

	public AllEntityScanner(AwsScanner scanner) {
		super(scanner);

		addScanners(AccountScanner.class, RegionScanner.class, VpcScanner.class, AvailabilityZoneScanner.class,
				SecurityGroupScanner.class, SubnetScanner.class, Ec2InstanceScanner.class,AmiScanner.class, LaunchConfigScanner.class,
				LaunchTemplateScanner.class, ElbScanner.class,ElbV2Scanner.class,ElbV2TargetGroupScanner.class, AsgScanner.class, LambdaFunctionScanner.class);
	}

}
