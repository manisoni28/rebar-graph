package rebar.graph.aws;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.util.Json;

public class VpcScannerGroupTest extends AwsIntegrationTest {

	Logger logger = LoggerFactory.getLogger(VpcScannerGroupTest.class);

	public VpcScannerGroupTest() {
		super();
	}

	@Override
	protected void beforeAll() {

		deleteAllAwsEntities();
		getAwsScanner().getEntityScanner(VpcScannerGroup.class).scan();
	

	}

	@Test
	public void testAccount() {
		
		Assertions.assertThat(
				getNeo4jDriver().cypher("match (a:AwsAccount) return a").findFirst().get().path("account").asText())
				.isEqualTo(getAwsScanner().getAccount());
		getNeo4jDriver().cypher("match (a:AwsAccount) return a").forEach(it -> {
			Json.logger().info(it);
			Assertions.assertThat(it.has("graphEntityDigest")).isTrue();
			Assertions.assertThat(it.path("graphEntityType").asText()).isEqualTo("AwsAccount");
			Assertions.assertThat(it.path("graphEntityGroup").asText()).isEqualTo("aws");
		});
	}



}
