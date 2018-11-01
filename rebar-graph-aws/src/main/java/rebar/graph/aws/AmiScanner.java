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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.amazonaws.internal.EC2MetadataClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.machinezoo.noexception.Exceptions;

import rebar.graph.core.GraphDB;
import rebar.util.Json;

public class AmiScanner extends AbstractEntityScanner<Image> {

	public AmiScanner(AwsScanner scanner) {
		super(scanner);
	}

	public void project(Image image) {
		ObjectNode n = toJson(image);
		n.set("account", n.get("ownerId"));

		getGraphDB().nodes("AwsAmi").idKey("arn").properties(n).merge();

	}

	@Override
	protected void gc(String type, long cutoff) {
		/**
		 * We need a custom gc() implementation for AMIs because we don't filter by account
		 */
		if (Strings.isNullOrEmpty(type)) {
			return;
		}
		Stopwatch sw = Stopwatch.createStarted();

		getGraphDB().nodes().whereAttributeLessThan(GraphDB.UPDATE_TS, cutoff).label(type)
				.id("region", getRegion().getName()).match().forEach(it -> {
					Exceptions.log(logger).run(() -> {
						logger.info("running gc on {}", it.path(GraphDB.ENTITY_TYPE).asText());
						scan(it);
					});
				});

		logger.info("gc for {} took {}ms", type, sw.elapsed(TimeUnit.MILLISECONDS));
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);

		DescribeImagesRequest request = new DescribeImagesRequest().withOwners("self");

		DescribeImagesResult result = ec2.describeImages(request);

		Set<String> scannedImageIds = Sets.newHashSet();
		result.getImages().forEach(image -> {
			scannedImageIds.add(image.getImageId());
			project(image);
		});

		// find the images that our EC2 instances are using, but skip if we've already
		// scanned them
		Set<String> imagesToScan = Sets.difference(findImagesInUse(), scannedImageIds);
		if (!imagesToScan.isEmpty()) {
			request = new DescribeImagesRequest();
			request.withImageIds(imagesToScan);

			result = ec2.describeImages(request);
			result.getImages().forEach(image -> {
				project(image);
			});
		}

		getGraphDB().nodes("AwsEc2Instance").id("region", getRegion().getName()).id("account", getAccount())
				.relationship("USES").on("imageId", "imageId").to("AwsAmi").id("region", getRegionName()).merge();
		gc("AwsAmi", ts);
	}

	@Override
	protected Optional<String> toArn(Image awsObject) {
		return Optional.ofNullable(String.format("arn:aws:ec2:%s::image/%s", getRegionName(), awsObject.getImageId()));

	}

	Set<String> findImagesInUse() {

		return getGraphDB().nodes("AwsEc2Instance").id("region", getRegionName()).id("account", getAccount()).match()
				.map(n -> n.path("imageId").asText()).collect(Collectors.toSet());

	}

	@Override
	public void scan(JsonNode entity) {

		String imageId = entity.path("imageId").asText();
		
		try {
			AmazonEC2 ec2 = getClient(AmazonEC2ClientBuilder.class);
			DescribeImagesRequest request = new DescribeImagesRequest().withImageIds(imageId);
			DescribeImagesResult result = ec2.describeImages(request);

			result.getImages().forEach(image -> {
				project(image);
			});
		} catch (AmazonEC2Exception e) {
			if ("InvalidAMIID.NotFound".equals(e.getErrorCode())) {
				getGraphDB().nodes("AwsAmi").id("region", getRegionName()).id("imageId", imageId).delete();
			}
		}

	}

}
