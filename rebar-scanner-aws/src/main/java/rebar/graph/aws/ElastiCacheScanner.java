package rebar.graph.aws;

import java.util.List;
import java.util.Optional;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClientBuilder;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.ReplicationGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.util.Json;

public class ElastiCacheScanner extends AwsEntityScanner<CacheCluster> {

	AmazonElastiCache getClient() {
		return getAwsScanner().getClient(AmazonElastiCacheClientBuilder.class);
	}

	protected void project(ReplicationGroup rg) {
		ObjectNode n = Json.objectMapper().valueToTree(rg);
		String arn = String.format("arn:aws:elasticache:%s:%s:cluster:%s", getRegionName(), getAccount(),
				rg.getReplicationGroupId());
		n.put("account", getAccount());
		n.put("region", getRegionName());
		n.put("graphEntityType", AwsEntityType.AwsCacheCluster.name());
		n.put("graphEntityGroup", "aws");
		n.put("arn", arn);
		n.put("configEndpointAddress", n.path("configurationEndpoint").path("address").asText());
		n.put("configEndpointAddress", n.path("configurationEndpoint").path("port").asText());
		n.put("configEndpoint", n.path("configurationEndpoint").path("address").asText() + ":"
				+ n.path("configurationEndpoint").path("port").asText());
		n.remove("configurationEndpoint");
		n.remove("pendingModifiedValues");

		awsGraphNodes(AwsEntityType.AwsCacheCluster.name()).idKey("arn").properties(n).merge();
	}

	protected void project(CacheCluster cluster) {

		ObjectNode n = toJson(cluster);
		
		n.path("cacheNodes").forEach(it -> {

			String uid = n.path("arn").asText() + "/" + it.path("cacheNodeId").asText();
			awsGraphNodes(AwsEntityType.AwsCacheClusterNode.name()).id("uid", uid).properties(it).merge();
		});

		n.remove("cacheNodes");
		awsGraphNodes(AwsEntityType.AwsCacheCluster.name()).idKey("arn").properties(n).merge();
		awsGraphNodes(AwsEntityType.AwsCacheCluster.name()).relationship("HAS").on("arn", "cacheClusterArn")
				.to(AwsEntityType.AwsCacheClusterNode.name()).merge();

	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		DescribeCacheClustersRequest request = new DescribeCacheClustersRequest()
				.withShowCacheClustersNotInReplicationGroups(false).withShowCacheNodeInfo(true);
		do {
			DescribeCacheClustersResult result = getClient().describeCacheClusters(request);
			request.setMarker(result.getMarker());

			result.getCacheClusters().forEach(it -> {
				logger.info("{} {}", it.getEngine(), it.getCacheClusterId());

				project(it);
			});
		} while (!Strings.isNullOrEmpty(request.getMarker()));

		getClient().describeReplicationGroups().getReplicationGroups().forEach(it -> {
			project(it);

		});
		mergeAccountOwner();
		gc(AwsEntityType.AwsCacheCluster.name(), ts);

	}

	@Override
	public void scan(JsonNode entity) {
		Json.logger().info("gc", entity);

	}

	@Override
	protected Optional<String> toArn(CacheCluster awsObject) {
		String name = awsObject.getReplicationGroupId();
		if (Strings.isNullOrEmpty(name)) {
			name = awsObject.getCacheClusterId();
		}

		String arn = String.format("arn:aws:elasticache:%s:%s:cluster:%s", getRegionName(), getAccount(), name);
		return Optional.ofNullable(arn);
	}

	@Override
	protected ObjectNode toJson(CacheCluster awsObject) {

		ObjectNode n = super.toJson(awsObject);

		if (!Strings.isNullOrEmpty(n.path("configurationEndpoint").path("address").asText(null))) {
			n.put("configEndpoint", n.path("configurationEndpoint").path("address").asText() + ":"
					+ n.path("configurationEndpoint").path("port").asText());
		} else {
			n.set("configEndpoint", null);
		}

		n.remove("configurationEndpoint");

		ArrayNode an = Json.arrayNode();
		awsObject.getSecurityGroups().forEach(sg -> {
			an.add(sg.getSecurityGroupId());
		});
		n.set("securityGroups", an);

		ArrayNode endpoints = Json.arrayNode();
		n.path("cacheNodes").forEach(it -> {

			String address = it.path("endpoint").path("address").asText(null);
			String port = it.path("endpoint").path("port").asText(null);
			if (!Strings.isNullOrEmpty(address)) {
				endpoints.add(address + ":" + port);
				
			}
			if (!Strings.isNullOrEmpty(address)) {
				String ep = address + ":" + port;
				ObjectNode.class.cast(it).put("endpoint", ep);
			}
			ObjectNode cacheNode = ObjectNode.class.cast(it);

			cacheNode.set("cacheClusterArn", n.path("arn"));
			cacheNode.put("graphEntityType", AwsEntityType.AwsCacheClusterNode.name());
			cacheNode.put("graphEntityGroup", "aws");
			cacheNode.put("account", getAccount());
			cacheNode.put("region", getRegionName());
		});
		n.set("cacheNodeEndpoints", endpoints);
		if (endpoints.size()==1) {
			n.set("primaryEndpoint",endpoints.get(0));
		}
		ArrayNode cacheSecurityGroups = Json.arrayNode();
		awsObject.getCacheSecurityGroups().forEach(sg -> {
			an.add(sg.getCacheSecurityGroupName());
		});
		n.set("cacheSecurityGroups", cacheSecurityGroups);

		n.set("cacheParameterGroupName", n.path("cacheParameterGroup").path("cacheParameterGroupName"));
		n.set("cacheNodeIdsToReboot", n.path("cacheParameterGroup").path("cacheNodeIdsToReboot"));

		n.remove("cacheParameterGroup");

		n.remove("pendingModifiedValues");
		n.put("name", !Strings.isNullOrEmpty(awsObject.getReplicationGroupId()) ? awsObject.getReplicationGroupId() : awsObject.getCacheClusterId());

		return n;
	}

	@Override
	public void scan(String id) {

		DescribeCacheClustersResult result = getClient()
				.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId(id));
		result.getCacheClusters().forEach(it -> {
			project(it);
		});
	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsCacheCluster;
	}

}
