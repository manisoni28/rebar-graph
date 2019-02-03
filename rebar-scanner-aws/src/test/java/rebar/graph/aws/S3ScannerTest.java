package rebar.graph.aws;

import org.junit.jupiter.api.Test;

import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableMap;

public class S3ScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {
		deleteAllAwsEntities();
	}
	@Test
	public void testIt() {
		
		AwsScanner scanner = getAwsScanner().getRebarGraph().newScanner(AwsScanner.class,ImmutableMap.of("region",Regions.US_WEST_2.name()));
	
		scanner.getEntityScanner(AccountScanner.class).scan();
		scanner.getEntityScanner(RegionScanner.class).scan();
		scanner.getEntityScanner(S3Scanner.class).scan();
	}

}
