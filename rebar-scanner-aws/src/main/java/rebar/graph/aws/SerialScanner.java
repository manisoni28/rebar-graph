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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

public class SerialScanner extends AbstractEntityScanner {

	List<Class<? extends AbstractEntityScanner>> scanners = Lists.newArrayList();

	

	public SerialScanner removeScanner(Class<? extends AbstractEntityScanner> scanner) {
		scanners.remove(scanner);
		return this;
	}
	
	public SerialScanner addScanners(List<Class<? extends AbstractEntityScanner>> scanners) {
		this.scanners.addAll(scanners);
		return this;
	}
	public SerialScanner addScanners(Class<? extends AbstractEntityScanner>... scanners) {
		if (scanners != null) {
			this.scanners.addAll(Arrays.asList(scanners));
		}
		return this;
	}
	protected Optional<String> toArn(Object awsEntity) {
		return Optional.empty();
	}

	@Override
	protected void doScan() {

		scanners.forEach(scanner -> {
			try {
				Constructor ctor = scanner.getConstructor();
				AbstractEntityScanner s = (AbstractEntityScanner) ctor.newInstance();
				s.init(getAwsScanner());
				s.scan();
			}
			catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
				maybeThrow(e);
			}
		});

	}

	@Override
	public void scan(JsonNode entity) {
		// do nothing

	}

	@Override
	public void scan(String id) {
		// do nothing
		
	}

}
