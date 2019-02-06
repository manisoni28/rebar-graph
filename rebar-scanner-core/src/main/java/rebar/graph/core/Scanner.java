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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.machinezoo.noexception.Exceptions;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import rebar.graph.neo4j.GraphDriver;
import rebar.graph.neo4j.GraphException;
import rebar.util.EnvConfig;
import rebar.util.RebarException;

public abstract class Scanner {

	static org.slf4j.Logger logger = LoggerFactory.getLogger(Scanner.class);

	static ObjectMapper mapper = new ObjectMapper();

	Boolean failOnError = null;
	

	MeterRegistry meterRegistry = Metrics.globalRegistry; 
	
	RebarGraph rebarGraph;
	
	public String getEntityGroup() {
		return getClass().getSimpleName().toLowerCase().replace("scanner", "");
		
	}
	public Scanner() {
		
		
	}
	public final void _init(RebarGraph g, Map<String,String> config)  throws Exception {
		Preconditions.checkNotNull(g);
		Preconditions.checkNotNull(config);
		this.rebarGraph = g;
		init(g,config);
	}
	protected abstract void init(RebarGraph g, Map<String,String> config) throws Exception ;
	
	public MeterRegistry metrics() {
		return meterRegistry;
	}

	public final EnvConfig getEnvConfig() {
		Preconditions.checkNotNull(getRebarGraph());
		return getRebarGraph().getEnvConfig();
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
		try {
			doScan();
		}
		catch(Exception e) {
			maybeThrow(e);
		}
		logger.info("end scan for {} ({}ms)",this,sw.elapsed(TimeUnit.MILLISECONDS));
	}
	
	protected abstract void doScan()  ;

	public void tryExecute(Invokable r) {
		try {
			r.invoke();
		} catch (Exception e) {
			if (isFailOnError()) {
				if (e instanceof RuntimeException ) {
					throw (RuntimeException) e;
				}
				else {
					Exceptions.sneak().handle(e); // try this bit of wizardry
				}
			} else {
				logger.warn("problem", e);
			}
		}
	}

	public boolean isFailOnError() {
		return failOnError!=null && failOnError==true;
	}

	public final void maybeThrow(Exception e) {
		if (isFailOnError()) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			else {
				throw new RebarException(e);
			}
		}
		else {
			logger.warn("unexpected exception",e);
		}
	}
	public RebarGraph getRebarGraph() {
		return rebarGraph;
	}

	public final void init(EntityScanner scanner) {
		Preconditions.checkNotNull(scanner);
		scanner.setScanner(this);
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

	public final void applyConstraints() {}
}
