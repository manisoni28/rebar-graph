package rebar.graph.aws;

import com.amazonaws.services.autoscaling.waiters.AmazonAutoScalingWaiters;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

public abstract class AbstractNetworkScanner<A> extends AwsEntityScanner<A,AmazonEC2Client> {




	

	@Override
	protected AmazonEC2Client getClient() {
		return getClient(AmazonEC2ClientBuilder.class);
	}
	

}
