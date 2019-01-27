package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class SnsScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {
		deleteAllAwsEntities();
	}
	@Test
	public void testIt() {
		getAwsScanner().getEntityScanner(AccountScanner.class).scan();
		getAwsScanner().getEntityScanner(RegionScanner.class).scan();
		getAwsScanner().getEntityScanner(SqsScanner.class).scan();
		getAwsScanner().getEntityScanner(LambdaFunctionScanner.class).scan();
		getAwsScanner().getEntityScanner(SnsScanner.class).scan();
	}

}
