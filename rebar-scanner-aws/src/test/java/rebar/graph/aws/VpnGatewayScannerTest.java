package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class VpnGatewayScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {

		super.beforeAll();

		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(NetworkScannerGroup.class).scan();
	}
	
	@Test
	public void testIt() {
		
		getAwsScanner().getEntityScanner(VpnGatewayScanner.class).scan("fizz");
		getAwsScanner().getEntityScanner(VpnGatewayScanner.class).scan("vpce-aabbc");
	}
}
