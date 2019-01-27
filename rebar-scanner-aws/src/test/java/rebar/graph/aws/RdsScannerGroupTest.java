package rebar.graph.aws;

import org.junit.jupiter.api.Test;

public class RdsScannerGroupTest extends AwsIntegrationTest {

	public RdsScannerGroupTest() {
		// TODO Auto-generated constructor stub
	}
	
	public void beforeAll() {
		deleteAllAwsEntities();
		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
		getAwsScanner().getEntityScanner(RdsScannerGroup.class).scan();
	}
	@Test
	public void testIt() {
		
	}



}
