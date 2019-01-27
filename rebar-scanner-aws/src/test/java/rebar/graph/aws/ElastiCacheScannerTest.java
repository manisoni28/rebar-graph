package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class ElastiCacheScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {
		
		super.beforeAll();
		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(ElastiCacheScanner.class).scan();
		
		
	}

	@Test
	public void testIt() {
		//match (a:AwsCacheCluster)--(b)  return a.name,a.arn,a.engine,a.primaryEndpointAddress,a.primaryEndpointPort,a.configEndpointAddress,a.configEndpointPort;
		
	}

}
