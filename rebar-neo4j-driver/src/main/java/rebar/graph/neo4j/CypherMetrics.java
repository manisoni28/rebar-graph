package rebar.graph.neo4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import rebar.util.Json;

public class CypherMetrics {

	public class StatementStats {
		String cypher;
		String key;
		AtomicLong executionCount = new AtomicLong();

		long startTs = System.currentTimeMillis();
		AtomicLong min = new AtomicLong(Long.MAX_VALUE);
		AtomicLong max = new AtomicLong();
		AtomicLong total = new AtomicLong();

		public void recordExecutionTime(long time) {
			executionCount.incrementAndGet();
			min.lazySet(Math.min(min.get(), time));
			max.lazySet(Long.max(max.get(), time));
			total.addAndGet(time);
		}

		public JsonNode toJson() {
			ObjectNode d = Json.objectNode();
			d.put("cypher", cypher);
			d.put("count", executionCount.get());
			d.put("min", min.get());
			d.put("max", max.get());
			d.put("ts", startTs);
			d.put("hash", key);
			return d;
		}
		
		public long getCount() {
			return executionCount.get();
		}
		public long getStartTs() {
			return startTs;
		}
		public long getMax() {
			return max.get();
		}
		public long getMin() {
			return executionCount.get()==0 ? 0L : min.get();
		}
	}


	Cache<String, StatementStats> metricsData = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
			.maximumSize(1000).removalListener(new CacheRemovalListener()).build();

	MeterRegistry meterRegistry = io.micrometer.core.instrument.Metrics.globalRegistry;
	GraphDriver driver;
	List<RemovalListener<String, StatementStats>> listeners = Lists.newCopyOnWriteArrayList();
	Timer cypherStatementTimer;
	class CacheRemovalListener implements RemovalListener<String, StatementStats> {

		@Override
		public void onRemoval(RemovalNotification<String, StatementStats> notification) {

			for (RemovalListener<String,StatementStats> listener: listeners) {
				listener.onRemoval(notification);
			}

		}

	}

	protected CypherMetrics(GraphDriver driver, MeterRegistry registry) {
		this.driver = driver;
		this.meterRegistry = registry;
		this.cypherStatementTimer = this.meterRegistry.timer("neo4jStatementExecution");
		
	}

	public GraphDriver getGraphDriver() {
		return driver;
	}

	public MeterRegistry getMeterRegistry() {
		return meterRegistry;
	}

	public void recordStatementExecution(String cypher, long executionTime) {

		String key = Hashing.farmHashFingerprint64().hashString(cypher, Charsets.UTF_8).toString();
		StatementStats m = metricsData.getIfPresent(key);
		if (m == null) {
			m = new StatementStats();
			m.key = key;
			m.cypher = cypher;
			metricsData.put(key, m);
			
		}

		m.recordExecutionTime(executionTime);
		cypherStatementTimer.record(executionTime, TimeUnit.MILLISECONDS);
	}

	
	public void addListener(RemovalListener<String, StatementStats> listener) {
		this.listeners.add(listener);
	}
	public void resetStats() {
		metricsData.invalidateAll();
	}
	
	public Stream<StatementStats> getStatementStats() {

		return ImmutableList.copyOf(metricsData.asMap().values()).stream();
		
	}
}
