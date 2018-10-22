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

import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public abstract class ScannerBuilder<X> {

	RebarGraph graph;

	public abstract X build();

	
	@SuppressWarnings("unchecked")
	public <T extends Scanner> T register(String name) {
		T scanner = (T) build();
		getRebarGraph().registerScanner((Class<T>) scanner.getClass(), name,
				(Supplier<T>) Suppliers.ofInstance(scanner));
		
		return scanner;
	}

	protected RebarGraph getRebarGraph() {
		return graph;
	}

	protected void setRebarGraph(RebarGraph rg) {
		this.graph = rg;
	}

}
