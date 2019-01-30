package rebar.graph.aws;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.policy.resources.SQSQueueResource;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rebar.util.Json;
import rebar.util.Sleep;

public class SqsConsumer {

	static Logger logger = LoggerFactory.getLogger(SqsConsumer.class);
	AwsScanner scanner;
	String name;
	String queueUrl;
	AmazonSQSClient client;
	AtomicLong failureCount = new AtomicLong();

	List<Consumer<JsonNode>> consumer = Lists.newCopyOnWriteArrayList();

	AtomicBoolean running = new AtomicBoolean();
	SqsConsumer() {

	}

	public SqsConsumer withAwsScanner(AwsScanner scanner) {
		this.scanner = scanner;
		return this;
	}

	public SqsConsumer withQueueName(String name) {

		this.name = name;
		return this;
	}

	public SqsConsumer withQueueUrl(String url) {

		this.queueUrl = url;
		return this;
	}

	AmazonSQSClient getClient() {
		if (client == null) {
			client = scanner.getClient(AmazonSQSClientBuilder.class);
		}
		return client;
	}

	private String getQueueUrl(String name) {
		AmazonSQSClient c = getClient();
		String url = c.getQueueUrl(name).getQueueUrl();
		logger.info("Resolved queue name={} to {}", name, url);
		return url;
	}

	JsonNode unwrapSns(JsonNode n) {
		try {
			if (n.path("Type").asText().equals("Notification") && n.has("TopicArn")) {
				return Json.objectMapper().readTree(n.path("message").asText());
			} else {
				return n;
			}
		} catch (Exception e) {
			return n;
		}
	}

	void handleMessage(Message m) {
		try {

			JsonNode n = Json.objectMapper().readTree(m.getBody());

			JsonNode payload = unwrapSns(n);
			consumer.forEach(it -> {
				it.accept(payload);
			});
		} catch (Exception e) {
			logger.warn("", e);
		}
	}

	public void start() {
	

		Thread t = new ThreadFactoryBuilder().setNameFormat("cloudwatch-evt-%d").setDaemon(true).build().newThread(this::consumerLoop);

		t.start();
	}

	long exponentialBackoff() {
		if (failureCount.get() > 0) {
			long timeout = (long) Math.pow(2, failureCount.get()) * 1000;

			timeout = Math.min(timeout, TimeUnit.MINUTES.toMillis(1));
			return timeout;
		}
		return 0;
	}

	public void startConsumer() {
		resolveQueueUrl();

	}

	void resolveQueueUrl() {
		if (!Strings.isNullOrEmpty(queueUrl)) {
			return;
		} else if (!Strings.isNullOrEmpty(name)) {
			queueUrl = getQueueUrl(name);
		}
		Preconditions.checkState(!Strings.isNullOrEmpty(queueUrl), "queue name or url must be set");

	}

	
	public boolean isRunning() {
		return running.get();
	}
	private void consumerLoop() {
		running.set(true);
		while (Strings.isNullOrEmpty(queueUrl)) {
			if (!running.get()) {
				return;
			}
			if (Strings.isNullOrEmpty(queueUrl)) {
				failureCount.incrementAndGet();
			} else {
				failureCount.set(0);
			}
			try {
				resolveQueueUrl();

			} catch (Exception e) {
				failureCount.incrementAndGet();
				long sleepTime = exponentialBackoff();
				logger.warn("failure to resolve queue url: " + e.toString()+" will retry in {} ms",sleepTime);
				Sleep.sleep(sleepTime);
			}
		}
		
		
		while (running.get()) {
			try {
				ReceiveMessageResult result = client.receiveMessage(new ReceiveMessageRequest().withWaitTimeSeconds(10)
						.withQueueUrl(queueUrl).withMaxNumberOfMessages(10));
				failureCount.set(0);
				DeleteMessageBatchRequest dmb = new DeleteMessageBatchRequest(queueUrl);
				logger.info("received {} messages on {}", result.getMessages().size(), queueUrl);
				result.getMessages().forEach(it -> {
					handleMessage(it);
					dmb.withEntries(new DeleteMessageBatchRequestEntry(it.getMessageId(), it.getReceiptHandle()));
				});
				if (!dmb.getEntries().isEmpty()) {
					// client.deleteMessageBatch(dmb);
				}

			} catch (Exception e) {
				failureCount.incrementAndGet();
				logger.warn("exception", e);
			}

			Sleep.sleep(exponentialBackoff());

		}

	}

	public SqsConsumer addConsumer(Consumer<JsonNode> n) {
		this.consumer.add(n);
		return this;
	}
}
