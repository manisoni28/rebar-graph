package rebar.graph.aws;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import rebar.util.Sleep;

public class ParallelScanner extends AwsEntityScanner {

	
	private static Map<String,ExecutorService> regionalExecutorMap = Maps.newConcurrentMap();
	
	public ParallelScanner() {
	
	}
	
	Set<Class<? extends AwsEntityScanner>> scanners =Sets.newHashSet();
	
	public ParallelScanner addScanner(Class<? extends AwsEntityScanner> scannerClass) { 
		return withScanner(scannerClass);
	}
	public ParallelScanner withScanner(Class<? extends AwsEntityScanner> scannerClass) {
		
		Preconditions.checkArgument(!ParallelScanner.class.isAssignableFrom(scannerClass));
		
		scanners.add(scannerClass);
		return this;
	}

	private synchronized void makeImmutable() {
		scanners = ImmutableSet.copyOf(scanners);
	}
	@Override
	protected final void doScan() {
		makeImmutable();
		List<CompletableFuture> futureList = Lists.newArrayList();
		for (Class<? extends AwsEntityScanner> x: scanners) {
			
			Runnable r = new Runnable() {

				@Override
				public void run() {
					AwsEntityScanner s = getAwsScanner().getEntityScanner(x);
					s.scan();
					logger.info("completed scan of {}",s);
				}
				
			};
			CompletableFuture future = CompletableFuture.runAsync(r, getExecutorServiceForRegion(getRegionName()));
			futureList.add(future);
		}
		
		
		
		CompletableFuture x = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
		
		while (!x.isDone()) {
			Sleep.sleep(100,TimeUnit.MILLISECONDS);
		}
		logger.info("complete!");
	}

	@Override
	public void doScan(JsonNode entity) {
		

	}

	@Override
	public void doScan(String id) {
	

	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.UNKNOWN;
	}

	/**
	 * Allows parallel concurrency by region.  This prevents a given region from locking up or hogging ability to make progress
	 * on other regions.
	 * @param region
	 * @return
	 */
	protected synchronized ExecutorService getExecutorServiceForRegion(String region) {
		ExecutorService executor = regionalExecutorMap.get(region);
		if (executor==null) {
			logger.info("creating executor to handle reagion: {}",region);
			executor = Executors.newWorkStealingPool(10);
			regionalExecutorMap.put(region, executor);
			
		}
		return executor;
	}
	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}
}
