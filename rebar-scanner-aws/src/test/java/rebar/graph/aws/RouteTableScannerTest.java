package rebar.graph.aws;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import rebar.util.Sleep;

public class RouteTableScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {

		super.beforeAll();

		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(NetworkScannerGroup.class).scan();
	}

	@Test
	public void testIt() {

		while (true == true) {
			getAwsScanner().getEntityScanner(RouteTableScanner.class).scan();

			Sleep.sleep(5, TimeUnit.SECONDS);
		}
	}

}
