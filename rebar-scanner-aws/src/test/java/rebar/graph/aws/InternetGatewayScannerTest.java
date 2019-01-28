package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class InternetGatewayScannerTest extends AwsIntegrationTest {


	@Override
	protected void beforeAll() {
	
		super.beforeAll();
		
		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(NetworkScannerGroup.class).scan();
	}

	@Test
	public void testIt() {
		getAwsScanner().getEntityScanner(InternetGatewayScanner.class).scan();
		getAwsScanner().getEntityScanner(InternetGatewayScanner.class).scan("foo");
	}

}
