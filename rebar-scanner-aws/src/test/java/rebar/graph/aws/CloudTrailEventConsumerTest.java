package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class CloudTrailEventConsumerTest extends AwsIntegrationTest {

	@Test
	public void testIt() {
		
	
	
		getAwsScanner().cloudTrailEvents().observableEvents("rebar-cloudwatch").forEach(it->{
			logger.info("processing event on thread {}: {}",Thread.currentThread().getName(),it);	
		});
		
		System.out.println();
		getAwsScanner().cloudTrailEvents().observableEvents("rebar-cloudwatch").forEach(it->{
			logger.info("processing event on thread {}: {}",Thread.currentThread().getName(),it);	
		});
	
	
	}

}
