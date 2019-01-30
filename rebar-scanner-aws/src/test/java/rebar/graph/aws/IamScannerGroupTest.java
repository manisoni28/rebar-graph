package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class IamScannerGroupTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {
	
		super.beforeAll();
		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(IamScannerGroup.class).scan();
	}

	
	@Test
	public void testIt() {
		
		getAwsScanner().getEntityScanner(IamUserScanner.class).scan("fizz");
	}

}
