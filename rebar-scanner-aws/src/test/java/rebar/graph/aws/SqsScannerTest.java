package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class SqsScannerTest extends AwsIntegrationTest {

	@Test
	public void testIt() {
		getAwsScanner().getEntityScanner(AccountScanner.class).scan();
		getAwsScanner().getEntityScanner(RegionScanner.class).scan();
		getAwsScanner().getEntityScanner(SqsScanner.class).scan();
	}

}
