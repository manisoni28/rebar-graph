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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.machinezoo.noexception.Exceptions;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class AllEntityScannerTest extends AwsIntegrationTest {

	@Test
	public void testIt() {
		getAwsScanner().getEntityScanner(AllEntityScanner.class).doScan();
	}

	@Test
	public void testCompleteness() {
		ScanResult result = new ClassGraph().enableClassInfo().whitelistPackages(getClass().getPackage().getName())
				.scan();

		List<Class> exclusions = ImmutableList.of(ElbListenerScanner.class, SerialScanner.class,
				AllEntityScanner.class);

		// Maybe we move this to a test. Just want to prevent scanners from being
		// forgotten.

		result.getSubclasses(AbstractEntityScanner.class.getName()).stream().filter(p -> !p.isAbstract()) 

				.forEach(it -> {
					try {

						Class clazz = Class.forName(it.getName());
						if (!exclusions.contains(clazz)) {

							Assertions.assertThat(getAwsScanner().getEntityScanner(AllEntityScanner.class).scanners)
									.contains((Class<? extends AbstractEntityScanner>) clazz);
						}
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				});

	}

}
