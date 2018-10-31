package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class AsgScannerTest extends AwsIntegrationTest {


	@Test
	public void testIT() {
	//	getAwsScanner().scan();
		getAwsScanner().getScanner(AsgScanner.class).scan();
	}

}
