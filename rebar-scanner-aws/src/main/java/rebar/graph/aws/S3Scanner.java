package rebar.graph.aws;

import java.util.List;
import java.util.Optional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.amazonaws.services.s3.model.RedirectRule;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.util.Json;

public class S3Scanner extends AwsEntityScanner<Bucket, AmazonS3Client> {

	protected AmazonS3Client getClient() {
		return getClient(AmazonS3ClientBuilder.class);
	}

	@Override
	protected void doScan() {
		long ts = getGraphDB().getTimestamp();
		ListBucketsRequest request = new ListBucketsRequest();

		List<Bucket> buckets = getClient().listBuckets(request);

		buckets.forEach(it -> {
			try {
				ObjectNode n = toJson(it);
				n.put("creationTs", it.getCreationDate().getTime());
				n.put("name", it.getName());
				n.set("ownerDisplayName", n.path("owner").path("displayName"));
				n.set("ownerId", n.path("owner").path("id"));
				n.remove("owner");
				fetchAttributes(it.getName(), n);
				getGraphDB().nodes(AwsEntityType.AwsS3Bucket.name()).id("name", it.getName()).properties(n).merge();
			} catch (RuntimeException e) {
				maybeThrow(e);
			}
		});
		gc(AwsEntityType.AwsS3Bucket.name(), ts);
		mergeAccountOwner(AwsEntityType.AwsS3Bucket);
	}

	protected void fetchAttributes(String bucketName, ObjectNode n) {

		logger.info("scanning bucket: {}", bucketName);

		String location = Strings.nullToEmpty(getClient().getBucketLocation(bucketName));
		if (location.equals("US")) {
			location = "us-east-1";
		}
		n.put("bucketRegion", location);
		n.put("region", location);
		try {
			if (isBucketInRegion(n)) {
				ServerSideEncryptionConfiguration cfg = getClient().getBucketEncryption(bucketName)
						.getServerSideEncryptionConfiguration();
				ObjectNode xx = (ObjectNode) Json.objectMapper().valueToTree(cfg);
			}

		} catch (AmazonS3Exception e) {
			String errorCode = Strings.nullToEmpty(e.getErrorCode());
			if (errorCode.equals("PermanentRedirect")
					|| errorCode.equals("ServerSideEncryptionConfigurationNotFoundError")) {
				// ignore
			} else {
				maybeThrow(e);
			}

		}

		if (isBucketInRegion(n)) {
			String policyText = getClient().getBucketPolicy(bucketName).getPolicyText();
			n.put("bucketPolicy", policyText);
		}

		if (n.path("bucketRegion").asText().equals(getRegionName())) {
			BucketWebsiteConfiguration bwc = getClient().getBucketWebsiteConfiguration(bucketName);

			if (bwc == null) {
				bwc = new BucketWebsiteConfiguration();
				String errorDocument = bwc.getErrorDocument();
				String indexDocumentSuffix = bwc.getIndexDocumentSuffix();
				n.put("websiteErrorDocument", errorDocument);
				n.put("websiteIndexDocumentSuffix", indexDocumentSuffix);
				RedirectRule rr = bwc.getRedirectAllRequestsTo();
				if (rr == null) {
					rr = new RedirectRule();
				}
				n.put("websiteRedirectHostName", rr.getHostName());
				n.put("websiteRedirectHttpCode", rr.getHttpRedirectCode());
				n.put("websiteRedirectProtocol", rr.getprotocol());
				n.put("websiteRedirectReplaceKeyPrefixWith", rr.getReplaceKeyPrefixWith());
				n.put("websiteRedirectReplaceKeyWith", rr.getReplaceKeyWith());
			}

		}

	}

	@Override
	public void doScan(JsonNode entity) {
		String type = entity.path("graphEntityType").asText();
		String account = entity.path("account").asText();

		if (type.equals("AwsS3Bucket") && account.equals(getAccount())) {
			doScan(entity.path("name").asText());
		}

	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		String name = id;
		ObjectNode n = Json.objectNode();
		n.put("name", name);
		n.put("graphEntityType", AwsEntityType.AwsS3Bucket.name());
		n.put("graphEntityGroup", "aws");
		n.put("arn", toArn(id));
		boolean exists = getClient().doesBucketExistV2(id);
		if (!exists) {
			getGraphDB().nodes(AwsEntityType.AwsS3Bucket.name()).id("name", id).id("account", getAccount()).delete();
			return;
		} else {
			fetchAttributes(id, n);
		}
		getGraphDB().nodes(AwsEntityType.AwsS3Bucket.name()).id("name", id).properties(n).merge();
		mergeAccountOwner(AwsEntityType.AwsS3Bucket);
	}

	protected boolean isBucketInRegion(JsonNode n) {
		return n != null && n.path("bucketRegion").asText().equals(getRegionName());
	}

	public String toArn(String bucketName) {
		return String.format("arn:aws:s3:::%s", bucketName);
	}

	@Override
	protected Optional<String> toArn(Bucket awsObject) {

		return Optional.ofNullable(toArn(awsObject.getName()));

	}

	@Override
	protected void doMergeRelationships() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void project(Bucket t) {
		throw new UnsupportedOperationException();

	}

	@Override
	public AwsEntityType getEntityType() {
		return AwsEntityType.AwsS3Bucket;
	}

}
