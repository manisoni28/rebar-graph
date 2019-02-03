package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class EgressOnlyInternetGatewayScannerTest extends AwsIntegrationTest {

	
	@Override
	protected void beforeAll() {
	
		super.beforeAll();
		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(NetworkScannerGroup.class).scan();
	}
	
	@Test
	public void testIt() {
		getAwsScanner().getEntityScanner(EgressOnlyInternetGatewayScanner.class).scan("fizz");
	}
}
