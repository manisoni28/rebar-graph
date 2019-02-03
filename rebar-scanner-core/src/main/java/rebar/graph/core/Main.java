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
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.machinezoo.noexception.Exceptions;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.micrometer.core.instrument.Metrics;
import rebar.graph.config.CoreSpringConfig;
import rebar.util.Sleep;

public class Main {
	static Logger logger = LoggerFactory.getLogger(Main.class);
	public static void main(String[] args) throws Exception {
		
		
		ConfigurableApplicationContext ctx = new SpringApplication(BaseConfig.class).run(args);

		
	
		Map<String,ScannerModule> modules = ctx.getBeansOfType(ScannerModule.class);
		
		if (modules.isEmpty()) {
			logger.warn("there are no modules");
		}
		

		RebarGraph g = ctx.getBean(RebarGraph.class);
		while (g.isRunning()) {
			Sleep.sleep(5,TimeUnit.SECONDS);
		}
	}

}
