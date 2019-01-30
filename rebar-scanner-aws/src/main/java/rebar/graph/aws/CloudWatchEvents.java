package rebar.graph.aws;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import rebar.util.EnvConfig;
import rebar.util.Json;

public class CloudWatchEvents implements Consumer<JsonNode> {

	static Logger logger = LoggerFactory.getLogger(CloudWatchEvents.class);

	AwsScanner scanner;
	SqsConsumer consumer;

	public CloudWatchEvents(AwsScanner scanner) {
		this.scanner = scanner;

	}

	public synchronized void stop() {
		if (consumer != null) {
			consumer.running.set(false);
		}
	}

	public synchronized void start() {
		if (consumer != null && consumer.isRunning()) {
			logger.warn("already started");
			return;
		}
		
		String queueName = new EnvConfig().get("AWS_CLOUDWATCH_QUEUE").orElse("rebar-events");
		logger.info("starting CloudWatch event queue monitor on queue name={}",queueName);
		consumer = new SqsConsumer().withQueueName(queueName).withAwsScanner(scanner);
		consumer.addConsumer(this);
		consumer.start();

	}

	Optional<String> extractAsgName(JsonNode n) {
		String source = n.path("source").asText();
		if (source.equals("aws.autoscaling")) {
			// match account;

			String asgName = n.path("detail").path("AutoScalingGroupName").asText();
			if (!Strings.isNullOrEmpty(asgName)) {
				return Optional.ofNullable(asgName);
			}
		}
		String asgName = n.path("AutoScalingGroupName").asText();
		if (!Strings.isNullOrEmpty(asgName)) {
			// match on AccountId
			return Optional.of(asgName);
		}

		return Optional.empty();
	}

	Optional<String> extractInstanceId(JsonNode n) {
		String instanceId = n.path("detail").path("instance-id").asText(null);
		if (!Strings.isNullOrEmpty(instanceId)) {
			return Optional.of(instanceId);
		}
		instanceId = n.path("detail").path("EC2InstanceId").asText();
		if (!Strings.isNullOrEmpty(instanceId)) {
			return Optional.ofNullable(instanceId);
		}
		instanceId = n.path("EC2InstanceId").asText();
		if (!Strings.isNullOrEmpty(instanceId)) {
			return Optional.ofNullable(instanceId);
		}
		String source = n.path("source").asText();
		if (source.equals("aws.s3") || source.equals("aws.ecr") || source.equals("aws.kms")) {
			return Optional.empty();
		}
		if (n.path("detail-type").asText().contains("CloudTrail")) {
			return Optional.empty();
		}
		if (n.path("detail-type").asText().contains("EBS Volume")) {
			return Optional.empty();
		}
		if (source.equals("aws.tag") && n.path("detail").path("resource-type").asText().equals("instance")) {
			String arn = n.path("resources").path(0).asText();

			Matcher m = Pattern.compile("arn:aws:ec2:.*:instance\\/(.*)$").matcher(arn);
			if (m.matches()) {
				String id = m.group(1);
				return Optional.of(id);
			}

		}
		instanceId = n.path("detail").path("tags").path("node.k8s.amazonaws.com/instance_id").asText();
		if (!Strings.isNullOrEmpty(instanceId)) {
			return Optional.ofNullable(instanceId);
		}

		return Optional.empty();
	}

	@Override
	public void accept(JsonNode n) {
		if (n == null) {
			return;
		}

		Optional<String> instanceId = extractInstanceId(n);
		Optional<String> asgName = extractAsgName(n);
		boolean handled = false;
		if (instanceId.isPresent()) {
			logger.info("rescan instance {}", instanceId.get());
			scanner.getEntityScanner(Ec2InstanceScanner.class).scanInstance(instanceId.get());
			handled = true;
		}
		if (asgName.isPresent()) {
			logger.info("rescan asg: {}", asgName.get());
			;
			scanner.getEntityScanner(AsgScanner.class).scanByName(asgName.get());
			handled = true;
		}
		if (handleCloudTrailS3Put(n)) {
			handled = true;

		}
		if (n.path("detail-type").asText().contains("EBS Volume")) {
			String event = n.path("detail").path("event").asText();
			String result = n.path("detail").path("result").asText();
			String resourceArn = n.path("resources").path(0).asText();
			logger.info("ebs volume notification: {}", resourceArn);
			handled = true;
		}
		if (n.path("detail-type").asText().contains("CloudTrail")) {
			logger.info("cloud trail event");
			handled = true;
		}
		if (!handled) {

			Json.logger().info("event not handled", n);

		}

	}

	boolean handleCloudTrailS3Put(JsonNode n) {
		if (!n.path("detail").path("sourceIPAddress").asText().toLowerCase().contains("cloudtrail")) {
			return false;
		}
		String bucket = n.path("detail").path("requestParameters").path("bucketName").asText();
		String key = n.path("detail").path("requestParameters").path("key").asText();

		logger.info("received cloudtrail s3 event bucket={} key={}", bucket, key);
		return true;
	}

}
