package rebar.graph.aws;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.util.Json;

public class CloudWatchEventsTest extends AwsIntegrationTest {

	class TestDataWriter implements Consumer<JsonNode> {

		@Override
		public void accept(JsonNode t) {
			try {
				String source = t.path("source").asText();
				String id = t.path("id").asText();
				String time = t.path("time").asText();
				if (Strings.isNullOrEmpty(id)) {
					id = t.path("RequestId").asText();
				}
				if (Strings.isNullOrEmpty(id)) {
					id = t.path("AcitivityId").asText();
				}

				if (Strings.isNullOrEmpty(id)) {
					Json.logger().info(t);
				} else {
					File f = new File("./src/test/resources/cloudwatch/events/" + id + ".json");
					logger.info("writing test data: {}", f.getAbsolutePath());
					Json.objectMapper().writerWithDefaultPrettyPrinter().writeValue(f, t);

				}
			} catch (Exception e) {
				logger.warn("", e);
			}
		}

	}

	@Test
	public void testIt() {

		getAwsScanner().cloudWatchEvents().start();
		getAwsScanner().cloudWatchEvents().stop();

	}

	@Test
	public void testExtractInstanceId() {
		getTestEvents().filter(p -> !CloudWatchEvents.extractInstanceId(p).isPresent()).forEach(it -> {

			String text = it.toString();
			if (text.contains("\"i-")) {

				if (text.contains("node.k8s.amazonaws.com/instance_id")) {
					// exception...this is really tagging the ENI
				} else {
					Json.logger().error("message contained an instanceId", it);
					Assertions.fail("message contained an instanceId");
				}
			}

		});
		
		getTestEvents().forEach(it->{
			CloudWatchEvents.extractInstanceId(it).ifPresent(x->{
				org.assertj.core.api.Assertions.assertThat(x).as("instanceId should start with i-").startsWith("i-");
			});
		});
	}

	/**
	 * Provides a bunch of real events to use for tests.
	 * 
	 * @return
	 */
	public Stream<JsonNode> getTestEvents() {

		List<JsonNode> list = Lists.newArrayList();
		for (File f : new File("./src/test/resources/cloudwatch/events").listFiles()) {
			try {
				JsonNode obj = Json.objectMapper().readTree(f);
				list.add(obj);

			} catch (Exception e) {
			}
		}
		return list.stream();

	}

	class AsgEventFilter implements Predicate<JsonNode> {

		@Override
		public boolean test(JsonNode t) {
			return t.path("Event").asText().startsWith("autoscaling:");
		}

	}

	class CloudTrailFilter implements Predicate<JsonNode> {

		@Override
		public boolean test(JsonNode t) {
			return t.path("detail-type").asText().contains("CloudTrail");
		}

	}

	class Ec2StateChangeFilter implements Predicate<JsonNode> {

		@Override
		public boolean test(JsonNode t) {
			if (!Strings.isNullOrEmpty(t.path("detail").path("instance-id").asText())) {
				return true;
			}
			return false;
		}

	}

}
