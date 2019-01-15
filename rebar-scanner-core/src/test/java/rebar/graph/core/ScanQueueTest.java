package rebar.graph.core;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import rebar.graph.core.Neo4jScanQueue.Subscription;
import rebar.util.Sleep;

public class ScanQueueTest extends Neo4jIntegrationTest {

	
	@AfterEach
	public void removeSubscriptions() {
		Neo4jScanQueue q = (Neo4jScanQueue) getRebarGraph().getScanQueue();
		
		List<Subscription> toBeRemoved = Lists.newArrayList();
		q.subs.forEach(s->{
			if (s.type.startsWith("junit")) {
				toBeRemoved.add(s);
			
			}
		});
		logger.info("removing test subscriptions: {}",toBeRemoved);
		q.subs.removeAll(toBeRemoved);

	}
	@Test
	public void testIt() throws InterruptedException {
		Assertions.assertThat(getRebarGraph()).isNotNull();
		Assertions.assertThat(getRebarGraph().getScanQueue()).isNotNull();
		
		
		ScanQueue q = getRebarGraph().getScanQueue();
		
		q.submit("junit", "foo","1","2","3");
		
		CountDownLatch latch = new CountDownLatch(1);
		Consumer<JsonNode> c = new Consumer<JsonNode>() {

			@Override
			public void accept(JsonNode t) {
				latch.countDown();
				
			}
		};
		
		q.subscribe(c, "junit","foo",null);
		
		Assertions.assertThat(latch.await(10, TimeUnit.SECONDS)).as("should receive event").isTrue();
		
		
	}

}
