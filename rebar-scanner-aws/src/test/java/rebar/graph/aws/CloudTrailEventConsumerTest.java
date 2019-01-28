package rebar.graph.aws;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import rebar.util.Json;

public class CloudTrailEventConsumerTest extends AwsIntegrationTest {

	
	
	@Test
	@Disabled
	public void testIt() {
		
	
	String bucketName = "";
		long count = getAwsScanner().cloudTrailEvents().observableEvents(bucketName).count().blockingGet();
		
		System.out.println(count);
		
	
	
	}

}
