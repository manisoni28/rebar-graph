package rebar.graph.aws;

import java.awt.AWTKeyStroke;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import rebar.graph.test.TestDataPolicy;
import rebar.util.Json;

public class VpcScannerGroupTest extends AwsIntegrationTest {

	Logger logger = LoggerFactory.getLogger(VpcScannerGroupTest.class);

	public VpcScannerGroupTest() {
		super(TestDataPolicy.NOOP);
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
