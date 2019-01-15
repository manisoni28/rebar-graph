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

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rebar.util.EnvConfig;

public abstract class AbstractGraphModule implements GraphModule {

	static Logger logger = LoggerFactory.getLogger(AbstractGraphModule.class);
	ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
			.setNameFormat("graph-%d").setUncaughtExceptionHandler(this::handleException).build());

	EnvConfig config = new EnvConfig();

	RebarGraph rebarGraph;

	public AbstractGraphModule() {

	}

	@Override
	public RebarGraph getRebarGraph() {
		return rebarGraph;
	}

	public ScheduledExecutorService getExecutor() {
		return executor;
	}

	public EnvConfig getConfig() {
		return config;
	}

	@VisibleForTesting
	boolean isFullScanEnabled(Optional<String> secs) {
		if (secs == null) {
			return true;
		}
		if (!secs.isPresent()) {
			return true;
		}

		if (secs.get().trim().equalsIgnoreCase("disabled")) {
			return false;
		}
		if (secs.get().trim().equalsIgnoreCase("off")) {
			return false;
		}
		try {
			long l = Long.parseLong(secs.get().trim());
			if (l <= 0) {
				return false;
			}
		} catch (Exception e) {
			// parse error is enabled
		}
		return true;
	}

	public boolean isFullScanEnabled() {
		return isFullScanEnabled(config.get(FULL_SCAN_INTERVAL));
	}

	@VisibleForTesting
	long getFullScanInterval(Optional<String> secs) {
		try {

			if (!secs.isPresent()) {
				return TimeUnit.MINUTES.toSeconds(5);
			}
			return Math.max(Long.parseLong(secs.get().trim()), 60);
		} catch (Exception e) {

			return TimeUnit.MINUTES.toSeconds(5);
		}
	}

	public long getFullScanInterval() {
		return getFullScanInterval(config.get(FULL_SCAN_INTERVAL));
	}

	protected void handleException(Thread thread, Throwable throwable) {
		logger.warn("uncaught exception", throwable);
	}
}
