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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.regions.Regions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.graph.core.ScannerBuilder;

public class AwsScannerBuilder extends ScannerBuilder<AwsScanner> {

	Logger logger = LoggerFactory.getLogger(AwsScannerBuilder.class);
	
	Regions region;
	List<Consumer<AwsClientBuilder<?, ?>>> configurers = Lists.newArrayList();

	public AwsScannerBuilder withCredentialsProvider(AWSCredentialsProvider p) {
		configurers.add(c -> {
			c.withCredentials(p);
		});
		return this;
	}

	public AwsScannerBuilder withRegion(Regions region) {
		this.region = region;
		return this;
	}

	public AwsScannerBuilder withConfig(Consumer<AwsClientBuilder<?, ?>> c) {
		configurers.add(c);
		return this;
	}

	@Override
	public AwsScanner build() {
		AwsScanner scanner = new AwsScanner(this);

		if (region == null) {
			String r = new DefaultAwsRegionProviderChain().getRegion();
			if (Strings.isNullOrEmpty(r)) {
				r = Regions.US_EAST_1.getName();
			}
			region = Regions.fromName(r);
			logger.info("region not specified defaulting to: {}", region.getName());
		}
		scanner.region = region;
		return scanner;
	}

}
