/**
 * Copyright 2018 Rob Schoening
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rebar.graph.aws;

import java.util.Optional;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import rebar.graph.core.GraphDB;
import rebar.util.RebarException;

public class Ec2InstanceScanner extends AbstractEntityScanner<Instance> {

	public Ec2InstanceScanner(AwsScanner scanner) {
		super(scanner);

	}

	public ObjectNode toJson(Instance instance, Reservation reservation) {

		ObjectNode n = toJson(instance);

		if (instance.getState() != null) {
			n.put("stateCode", instance.getState().getCode());
			n.put("stateName", instance.getState().getName());
		}
		if (instance.getStateReason() != null) {
			n.put("stateReasonCode", instance.getStateReason().getCode());
			n.put("stateReasonMessage", instance.getStateReason().getMessage());
		}

		if (reservation != null) {
			n.put("reservationOwnerId", reservation.getOwnerId());
			n.put("reservationRequesterId", reservation.getRequesterId());
			n.put("reservationId", reservation.getReservationId());
		}

		return n;

	}

	
	
	void project(Reservation r, Instance instance) {

		ObjectNode n = toJson(instance, r);

		getGraphDB().nodes().label("AwsEc2Instance").idKey("arn").properties(n).merge();

		String subnetId = instance.getSubnetId();
		if (!Strings.isNullOrEmpty(subnetId)) {
			getGraphDB().nodes("AwsEc2Instance").id("arn", n.get("arn").asText()).relationship("RESIDES_IN")
					.to("AwsSubnet")
					.id("subnetId", instance.getSubnetId(), "region", getRegionName(), "account", getAccount()).merge();
		}
	}

	protected Optional<String> toArn(Instance instance) {
		return Optional.of(String.format("arn:aws:ec2:%s:%s:instance/%s", getRegionName(), getAccount(), instance.getInstanceId()));
	}

	public void scanInstance(String instanceId) {
		try {
			AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);
			DescribeInstancesRequest request = new DescribeInstancesRequest();
			request.withInstanceIds(instanceId);

			do {
				DescribeInstancesResult result = ec2.describeInstances(request);
				result.getReservations().forEach(r -> {
					r.getInstances().forEach(i -> {
						tryExecute(() -> project(r, i));
					});
				});
				request = request.withNextToken(result.getNextToken());
			} while (!Strings.isNullOrEmpty(request.getNextToken()));
		} catch (AmazonEC2Exception e) {
			if (ImmutableSet.of("InvalidInstanceID.Malformed", "InvalidInstanceID.NotFound")
					.contains(e.getErrorCode())) {
				getGraphDB().nodes().label("AwsEc2Instance").id("instanceId", instanceId, "account", getAccount())
						.delete();
			} else {
				throw e;
			}
		}
	}

	@Override
	public void doScan() {

		long ts = System.currentTimeMillis();
		AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);

		DescribeInstancesRequest request = new DescribeInstancesRequest();

		do {
			DescribeInstancesResult result = ec2.describeInstances(request);
			result.getReservations().forEach(r -> {

				r.getInstances().forEach(it -> {
					tryExecute(() -> project(r, it));
				});
			});
			request.setNextToken(result.getNextToken());
		} while (!Strings.isNullOrEmpty(request.getNextToken()));

		gc("AwsEc2Instance", ts);

	}

	@Override
	public void scan(JsonNode entity) {
		assertEntityOwner(entity);

		if (isEntityType(entity, "AwsEc2Instance")) {
			String instanceId = entity.path("instanceId").asText();
			scanInstance(instanceId);

		} else {
			throw new RebarException("cannot process entity type: " + entity.path(GraphDB.ENTITY_TYPE).asText());
		}
	}

}
