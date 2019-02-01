package rebar.graph.aws;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.util.Json;

public class SqsScanner extends AwsEntityScanner<Map<String, String>,AmazonSQSClient> {

	
	public AmazonSQSClient getClient() {
		return getClient(AmazonSQSClientBuilder.class);
	}
	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		AmazonSQS client = getClient(AmazonSQSClientBuilder.class);
		ListQueuesResult result = client.listQueues();

		for (String url : result.getQueueUrls()) {
			try {
				scanQueueUrl(url);
			} catch (RuntimeException e) {
				maybeThrow(e);
			}
		}

		gc(AwsEntityType.AwsSqsQueue.name(), ts);
	}

	@Override
	public void doScan(JsonNode entity) {
		if (isEntityOwner(entity)) {

			String queueUrl = entity.path("url").asText();

			scanQueueUrl(queueUrl);
		}

	}

	protected void project(String url, Map<String, String> attrs) {
		ObjectNode n = toJson(attrs);
		n.put("url", url);

		awsGraphNodes(AwsEntityType.AwsSqsQueue.name()).idKey("arn").properties(n).merge();

	}

	private void mergeAccountOwnerRelationshipByUrl(String url) {
		getGraphDB().nodes(AwsEntityType.AwsAccount.name()).id("account", getAccount()).relationship("HAS")
				.on("account", "account").to(AwsEntityType.AwsSqsQueue.name()).id("url", url).id("account", getAccount()).merge();
	}
	private void mergeAccountOwnerRelationship() {
		getGraphDB().nodes(AwsEntityType.AwsAccount.name()).id("account", getAccount()).relationship("HAS")
				.on("account", "account").to(AwsEntityType.AwsSqsQueue.name()).id("account", getAccount()).merge();
	}

	public void scanQueueUrl(String url) {

		try {
			checkScanArgument(url);
			AmazonSQS client = getClient(AmazonSQSClientBuilder.class);
			GetQueueAttributesResult result = client.getQueueAttributes(url, Lists.newArrayList("All"));

			project(url, result.getAttributes());

		} catch (QueueDoesNotExistException e) {
			awsGraphNodes(AwsEntityType.AwsSqsQueue.name()).id("url",url).id("account",getAccount()).id("region",getRegionName()).delete();
		}

		mergeAccountOwnerRelationshipByUrl(url);

	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		if (id.startsWith("arn")) {
			throw new IllegalArgumentException("arn not supported");
		}
		if (id.startsWith("https")) {
			scanQueueUrl(id);
		} else {
			throw new IllegalArgumentException("invalid argument: " + id);
		}
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsSqsQueue;
	}

	@Override
	protected Optional<String> toArn(Map<String, String> awsObject) {
		return Optional.ofNullable(awsObject.get("queueArn"));
	}

	@Override
	protected ObjectNode toJson(Map<String, String> attrs) {
		ObjectNode n = Json.objectNode();

		Json.objectMapper().convertValue(attrs, ObjectNode.class).fields().forEachRemaining(it -> {
			String key = it.getKey().substring(0, 1).toLowerCase() + it.getKey().substring(1);
			n.set(key, it.getValue());
		});

		n.put("account", getAccount());
		n.put("region", getRegionName());
		n.set("arn", n.path("queueArn"));
		n.put("graphEntityType", AwsEntityType.AwsSqsQueue.name());
		n.put("graphEntityGroup", "aws");

		n.put("graphEntityType", AwsEntityType.AwsSqsQueue.name());
		n.put("graphEntityGroup", "aws");

		String arn = attrs.get("arn");
		if (!Strings.isNullOrEmpty(arn)) {
			List<String> list = Splitter.on(":").splitToList(arn);
			String name = list.get(list.size() - 1);
			n.put("name", name);
		}
		return n;
	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void project(Map<String, String> t) {
		throw new UnsupportedOperationException();
		
	}

}
