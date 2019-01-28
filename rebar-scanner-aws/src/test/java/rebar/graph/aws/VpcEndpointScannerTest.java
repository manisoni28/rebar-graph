package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class VpcEndpointScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {

		super.beforeAll();

		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(NetworkScannerGroup.class).scan();
	}
	
	@Test
	public void testIt() {
		
		getAwsScanner().getEntityScanner(VpcEndpointScanner.class).scan("fizz");
		getAwsScanner().getEntityScanner(VpcEndpointScanner.class).scan("vpce-aabbc");
	}

}
