package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class ApiGatewayScannerTest extends AwsIntegrationTest {

	
	
	@Override
	protected void beforeAll() {
	
		super.beforeAll();
		
		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(ApiGatewayRestApiScanner.class).scan();
	}

	@Test
	public void testIt() {
		
	}

}
