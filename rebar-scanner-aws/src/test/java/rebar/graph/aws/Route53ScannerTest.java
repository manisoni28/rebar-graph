package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class Route53ScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {
		deleteAllAwsEntities();
	}
	@Test
	public void testIt() {
		
		getAwsScanner().getEntityScanner(AccountScanner.class).scan();
		Route53Scanner scanner = getAwsScanner().getEntityScanner(Route53Scanner.class);
		scanner.scan();
		
		scanner.doScan("foz");
	}

}
