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
package rebar.graph.core;

import java.util.concurrent.atomic.AtomicInteger;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.machinezoo.noexception.Exceptions;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.micrometer.core.instrument.Metrics;

public class Main  {

	public static void main(String[] args) {
		
	

		RebarGraph g = new RebarGraph.Builder().build();

		ScanResult result = new ClassGraph().enableClassInfo().whitelistPackages("rebar.graph").scan();

		AtomicInteger count = new AtomicInteger(0);
		result.getClassesImplementing(GraphModule.class.getName()).stream().filter(p -> !p.isAbstract()).forEach(it -> {

			Exceptions.sneak().run(() -> {
				AbstractGraphModule m = (AbstractGraphModule) Class.forName(it.getName()).newInstance();
				
				m.rebarGraph = g;

				m.registerScanner(m.getScannerType());
				
				Thread thread = new ThreadFactoryBuilder().setNameFormat(it.getSimpleName() + "-%d").build()
						.newThread(m);

				thread.start();
				count.incrementAndGet();
			});

			
		});

		if (count.get()==0) {
			throw new IllegalStateException("RebarGraphModule implementation not available");
		}
	}

}
