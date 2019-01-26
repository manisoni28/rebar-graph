package rebar.graph.aws;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.amazonaws.services.apigateway.model.NotFoundException;
import com.amazonaws.services.s3.model.GetBucketNotificationConfigurationRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.GetTopicAttributesResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.util.Json;

public class SnsScanner extends AwsEntityScanner<Topic> {

	Pattern SUBSCRIPTION_ARN_REGEX = Pattern.compile("arn\\:aws\\:sns\\:(.+?)\\:(.+?)\\:(.+?)\\:(.*)");

	protected void projectTopicAttributes(String topicArn, Map<String, String> attrs) {

		ObjectNode n = Json.objectNode();
		attrs.forEach((k, v) -> {
			// since these are literal soft key-value pairs, we do NOT change case
			n.put(k, v);
		});
		n.put("region", getRegionName());
		n.put("account", getAccount());
		n.put("arn", topicArn);
		n.put("graphEntityType", AwsEntityType.AwsSnsTopic.name());
		n.put("graphEntityGroup", "aws");

		awsGraphNodes(AwsEntityType.AwsSnsTopic.name()).idKey("arn").properties(n);
	}

	protected void project(Topic topic) {

		ObjectNode n = toJson(topic);

		getGraphDB().nodes(AwsEntityType.AwsSnsTopic.name()).idKey("arn").properties(n).merge();
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		AmazonSNS sns = getClient(AmazonSNSClientBuilder.class);
		ListTopicsRequest request = new ListTopicsRequest();
		do {
			ListTopicsResult result = sns.listTopics(request);

			result.getTopics().forEach(it -> {
				tryExecute(() -> {
					project(it);
				});

			});

			result.getTopics().forEach(it -> {

				tryExecute(() -> {
					scanTopicArn(it.getTopicArn());
				});
				tryExecute(() -> {
					scanSubscriptions(it);
				});

			});
			request.setNextToken(result.getNextToken());
		} while (!Strings.isNullOrEmpty(request.getNextToken()));

		// Note that AwsEntityType.AwsSnsSubscription does not need explicit gc here
		gc(AwsEntityType.AwsSnsTopic.name(), ts);

		mergeAccountOwner(AwsEntityType.AwsSnsTopic);
		mergeAccountOwner(AwsEntityType.AwsSnsSubscription);
	}

	@Override
	public void scan(JsonNode entity) {

		Json.logger().info("gc", entity);
		if (isEntityOwner(entity)) {
			String arn = entity.path("arn").asText();
			String entityType = entity.path("graphEntityType").asText();
			if (entityType.equals(AwsEntityType.AwsSnsSubscription.name())) {

			} else if (entityType.equals(AwsEntityType.AwsSnsTopic.name())) {
				scan(arn);
			}
		}
	}

	@Override
	protected Optional<String> toArn(Topic awsObject) {
		return Optional.of(awsObject.getTopicArn());
	}

	protected void scanTopicArn(String arn) {
		try {

			GetTopicAttributesResult result = getClient().getTopicAttributes(arn);

			projectTopicAttributes(arn, result.getAttributes());
		} catch (com.amazonaws.services.sns.model.NotFoundException e) {
			awsGraphNodes(AwsEntityType.AwsSnsTopic.name()).id("arn", arn).id("region", getRegionName())
					.id("account", getAccount()).delete();
		}

	}

	@Override
	public void scan(String id) {

		if (SUBSCRIPTION_ARN_REGEX.matcher(id).matches()) {
			logger.warn("cannot scan by subscription arn: {}", id);
		} else if (id.startsWith("arn:aws:sns:")) {

			scanTopicArn(id);
		} else {
			throw new IllegalArgumentException(id);
		}
	}

	@Override
	public AwsEntityType getEntityType() {

		return AwsEntityType.AwsSnsTopic;
	}

	public AmazonSNS getClient() {
		return getClient(AmazonSNSClientBuilder.class);
	}

	private void scanSubscriptions(Topic topic) {

		ListSubscriptionsByTopicResult result = getClient().listSubscriptionsByTopic(topic.getTopicArn());
		String token = null;
		List<String> subscriptions = Lists.newArrayList();
		do {
			token = result.getNextToken();
			for (Subscription subscription : result.getSubscriptions()) {
				String subArn = subscription.getSubscriptionArn();
				projectSubscription(topic, subscription);
				mergeSubcriptionTarget(subscription);
				subscriptions.add(subscription.getSubscriptionArn());
			}
			result = getClient().listSubscriptionsByTopic(topic.getTopicArn(), token);

		} while ((!Strings.isNullOrEmpty(token)) && (!token.equals("null")));

		getGraphDB().getNeo4jDriver().cypher(
				"match (a:AwsSnsTopic {arn:{arn}})--(s:AwsSnsSubscription) where not s.arn in {subs} detach delete s")
				.param("subs", subscriptions)
				.param("arn", topic.getTopicArn()).exec();

	

	}

	private void mergeSubcriptionTarget(Subscription s) {

		
		String endpoint = Strings.nullToEmpty(s.getEndpoint());
		if (s.getEndpoint().startsWith("arn:aws:sqs")) {
			getGraphDB().nodes(AwsEntityType.AwsSnsSubscription.name()).id("arn", s.getSubscriptionArn())
					.relationship("HAS").to(AwsEntityType.AwsSqsQueue.name()).id("arn", s.getEndpoint()).merge();
		} 
		else if (s.getEndpoint().startsWith("arn:aws:lambda")) {
			getGraphDB().nodes(AwsEntityType.AwsSnsSubscription.name()).id("arn", s.getSubscriptionArn())
			.relationship("HAS").to(AwsEntityType.AwsLambdaFunction.name()).id("arn", s.getEndpoint()).merge();	
		}
		else if (s.getSubscriptionArn().equals("PendingConfirmation")) {
			// do nothing
		}
		else {
			logger.info("relationship to {} (arn={}) from {} not implemented", s.getProtocol(),s.getEndpoint(),s.getSubscriptionArn());
			
		}
	}

	private void projectSubscription(Topic topic, Subscription subscription) {

		if (!Strings.nullToEmpty(subscription.getSubscriptionArn()).startsWith("aws:arn")) {
			// subscriptions in PendingConfirmation state will have the arn set to 'PendingConfirmation'
			return;
		}
		ObjectNode n = Json.objectNode();
		n.put("topicArn", subscription.getTopicArn());
		n.put("endpoint", subscription.getEndpoint());
		n.put("protocol", subscription.getProtocol());
		n.put("owner", subscription.getOwner());
		n.put("region", getRegion().getName());
		n.put("account", getAccount());
		n.put("arn", subscription.getSubscriptionArn());
		n.put("graphEntityType", AwsEntityType.AwsSnsSubscription.name());
		n.put("graphEntityGroup", "aws");
		
		awsGraphNodes("AwsSnsSubscription").id("arn", subscription.getSubscriptionArn()).properties(n).merge();

		String cypher = "match (a:AwsSnsSubscription {arn:{subscriptionArn}}), (t:AwsSnsTopic {arn:{topicArn}}) MERGE (t)-[r:HAS]->(a) ";

		getGraphDB().getNeo4jDriver().cypher(cypher).param("subscriptionArn", subscription.getSubscriptionArn())
				.param("topicArn", topic.getTopicArn()).exec();

		
	}
}
