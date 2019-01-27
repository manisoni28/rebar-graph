package rebar.graph.aws;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.amazonaws.regions.Regions;

public class ParallelScannerTest extends AwsIntegrationTest {

	@Test
	public void testIt() {
		ParallelScanner a = getAwsScanner().getEntityScanner(ParallelScanner.class);
		
		ParallelScanner b = getAwsScanner().getEntityScanner(ParallelScanner.class);
		
		Assertions.assertThat(a).isNotSameAs(b);
		
		// executor from the same scanner and region should be the same executor
		Assertions.assertThat(a.getExecutorServiceForRegion(Regions.US_EAST_1.name())).isSameAs(a.getExecutorServiceForRegion(Regions.US_EAST_1.name()));
		
		// executor from the same scanner, but different region, should be a different executor
		Assertions.assertThat(a.getExecutorServiceForRegion(Regions.US_EAST_1.name())).isNotSameAs(a.getExecutorServiceForRegion(Regions.US_WEST_2.name()));
	
		// executor from different scanner but same region should be the same executor (i.e. globally shared)
		Assertions.assertThat(a.getExecutorServiceForRegion(Regions.US_EAST_1.name())).isSameAs(b.getExecutorServiceForRegion(Regions.US_EAST_1.name()));
		
		// executor for different regions should not be the same
		Assertions.assertThat(a.getExecutorServiceForRegion(Regions.US_EAST_1.name())).isNotSameAs(b.getExecutorServiceForRegion(Regions.US_WEST_2.name()));
	}
	
	

}
