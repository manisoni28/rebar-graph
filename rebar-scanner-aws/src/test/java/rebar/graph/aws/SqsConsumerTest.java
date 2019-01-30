package rebar.graph.aws;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import rebar.util.Json;
import rebar.util.Sleep;
import rebar.util.Json.JsonLogger;

public class SqsConsumerTest extends AwsIntegrationTest {

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
		

		 Sleep.sleep(5, TimeUnit.MINUTES);
	}


	@Test
	public void testX() {

		List<JsonNode> list = Lists.newArrayList();
		for (File f : new File("./src/test/resources/cloudwatch/events").listFiles()) {
			try {
				JsonNode obj = Json.objectMapper().readTree(f);
				list.add(obj);

			} catch (Exception e) {
			}
		}

		
	
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
