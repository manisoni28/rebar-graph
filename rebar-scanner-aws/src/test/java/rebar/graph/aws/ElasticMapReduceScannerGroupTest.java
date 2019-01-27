package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class ElasticMapReduceScannerGroupTest extends AwsIntegrationTest {


	
	@Override
	protected void beforeAll() {

		super.beforeAll();
		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(ElasticMapReduceClusterScanner.class).scan();
	}

	@Test
	public void testIT() {
		
	}
	

}
