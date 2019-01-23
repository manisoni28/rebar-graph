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
package rebar.graph.core;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

import rebar.graph.neo4j.GraphDriver;
import rebar.graph.neo4j.GraphException;

public abstract class Scanner {

	static org.slf4j.Logger logger = LoggerFactory.getLogger(Scanner.class);

	static ObjectMapper mapper = new ObjectMapper();
	ScannerBuilder<? extends Scanner> scannerBuilder;

	public Scanner(ScannerBuilder<? extends Scanner> builder) {
		this.scannerBuilder = builder;
	}

	


	private GraphOperation getOperation(Class<? extends GraphOperation> operationClass) {

		try {

			
			return  operationClass.newInstance();
		} catch (InstantiationException| IllegalAccessException e) {
			throw new GraphException(e);
		}

	}
	
	

	public Stream<JsonNode> execGraphOperation(Class<? extends GraphOperation> operation, JsonNode arg) {

			return getOperation(operation).exec(this,arg, getRebarGraph().getGraphDB().getNeo4jDriver());
		
	}
	
	public final void queueScan(String b, String c, String d) {}
	
	public abstract void scan(String scannerType, String a, String b, String c, String d);
	
	public final void scan() {
		Stopwatch sw = Stopwatch.createStarted();
		logger.info("begin scan for {}",this);
		doScan();
		logger.info("end scan for {} ({}ms)",this,sw.elapsed(TimeUnit.MILLISECONDS));
	}
	
	protected abstract void doScan();

	public void tryExecute(Runnable r) {
		try {
			r.run();
		} catch (RuntimeException e) {
			if (isFailOnError()) {
				throw e;
			} else {
				logger.warn("problem", e);
			}
		}
	}

	protected boolean isFailOnError() {
		return false;
	}


	public RebarGraph getRebarGraph() {
		return getScannerBuilder().getRebarGraph();
	}

	protected ScannerBuilder<? extends Scanner> getScannerBuilder() {
		return scannerBuilder;
	}
	
	public String getScannerType() {
		return getClass().getPackage().getName().replace("rebar.graph.", "");
	}
	public final GraphDriver getGraphDriver() {
		return getNeo4jDriver();
	}
	public final GraphDriver getNeo4jDriver() {
		return getGraphDB().getNeo4jDriver();
	}
	public final GraphDB getGraphDB() {
		return getRebarGraph().getGraphDB();
	}

}
