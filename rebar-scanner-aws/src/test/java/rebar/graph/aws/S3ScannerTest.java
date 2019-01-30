package rebar.graph.aws;

import org.junit.jupiter.api.Test;

import com.amazonaws.regions.Regions;

public class S3ScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {
		deleteAllAwsEntities();
	}
	@Test
	public void testIt() {
		
		AwsScanner scanner = getAwsScanner().getRebarGraph().createBuilder(AwsScannerBuilder.class).withRegion(Regions.US_WEST_2).build();
	
		scanner.getEntityScanner(AccountScanner.class).scan();
		scanner.getEntityScanner(RegionScanner.class).scan();
		scanner.getEntityScanner(S3Scanner.class).scan();
	}

}
