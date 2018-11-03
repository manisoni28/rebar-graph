package rebar.graph.aws;

import com.amazonaws.auth.policy.actions.ElasticLoadBalancingActions;
import com.amazonaws.services.ec2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupAttributesRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.util.Json;

public class ElbV2Scanner extends AbstractEntityScanner<LoadBalancer> {

	public ElbV2Scanner(AwsScanner scanner) {
		super(scanner);

	}

	void project(LoadBalancer lb) {
		ObjectNode n = toJson(lb);
		n.put("name", lb.getLoadBalancerName());
		n.put("arn", lb.getLoadBalancerArn());
		n.put("stateCode", lb.getState() != null ? lb.getState().getCode() : null);
		n.put("stateReason", lb.getState() != null ? lb.getState().getReason() : null);
		n.remove("state");
		ArrayNode zones = Json.arrayNode();
		ArrayNode subnets = Json.arrayNode();
		n.path("availabilityZones").forEach(az->{
			zones.add(az.path("zoneName").asText());
			subnets.add(az.path("subnetId").asText());
			// there is other nested information that (loadBalancerAddresses) that we may want to pull out
		});
		n.set("availabilityZones", zones);
		n.set("subnets", subnets);
		

			getGraphDB().nodes("AwsLoadBalancer").idKey("name", "region", "account").withTagPrefixes(TAG_PREFIXES)
					.properties(n).merge();
		
	}
	
	


	@Override
	protected void doScan() {
		
		long ts = getGraphDB().getTimestamp();
		AmazonElasticLoadBalancingClient client = getAwsScanner()
				.getClient(AmazonElasticLoadBalancingClientBuilder.class);

		
		DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
		do {
			DescribeLoadBalancersResult result = client.describeLoadBalancers(request);
			result.getLoadBalancers().forEach(lb -> {
				project(lb);
			});
			request.setMarker(result.getNextMarker());
		} while (!Strings.isNullOrEmpty(request.getMarker()));

		
		gc("AwsLoadBalancer",ts);

		
		getAwsScanner().getScanner(ElbV2TargetGroupScanner.class).scan();
		getAwsScanner().getScanner(ElbV2ListenerScanner.class).scan();
	}

	@Override
	public void scan(JsonNode entity) {

		String name = entity.path("name").asText();
		try {
			AmazonElasticLoadBalancingClient client = getAwsScanner()
					.getClient(AmazonElasticLoadBalancingClientBuilder.class);

			DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
			request.withNames(entity.path("name").asText());
			DescribeLoadBalancersResult result = client.describeLoadBalancers(request);
			result.getLoadBalancers().forEach(lb -> {
				project(lb);
			});
		} catch (LoadBalancerNotFoundException e) {
			getGraphDB().nodes("AwsLoadBalancer").id("name", name).id("region", getRegionName()).id("account", getAccount())
					.delete();
			
		}

	}

}
