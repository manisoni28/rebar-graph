package rebar.graph.aws;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import rebar.util.Sleep;

public class ParallelScanner extends AwsEntityScanner {

	static ExecutorService globalExecutor = Executors.newWorkStealingPool(20);
	public ParallelScanner() {
	
	}
	Set<Class<? extends AwsEntityScanner>> scanners =Sets.newCopyOnWriteArraySet();
	
	public ParallelScanner withScanner(Class<? extends AwsEntityScanner> scannerClass) {
		
		Preconditions.checkArgument(!ParallelScanner.class.isAssignableFrom(scannerClass));
		
		scanners.add(scannerClass);
		return this;
	}

	@Override
	protected void doScan() {
		
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
			CompletableFuture future = CompletableFuture.runAsync(r, globalExecutor);
			futureList.add(future);
		}
		
		
		
		CompletableFuture x = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
		
		while (!x.isDone()) {
			Sleep.sleep(100,TimeUnit.MILLISECONDS);
		}
		logger.info("complete!");
	}

	@Override
	public void scan(JsonNode entity) {
		

	}

	@Override
	public void scan(String id) {
	

	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.UNKNOWN;
	}

}
