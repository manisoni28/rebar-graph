package rebar.graph.aws;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;
import rebar.util.Json;
import rebar.util.RebarException;

public class CloudTrailEvents {


	Logger logger = org.slf4j.LoggerFactory.getLogger(CloudTrailEvents.class);
	AwsScanner scanner;

	final long HORIZON_HOURS = 6;

	public static enum FileState {
		PENDING, COMPLETE, FAILED
	}

	CloudTrailEvents(AwsScanner scanner) {
		this.scanner = scanner;
	}

	private void readS3(String bucketName, String key, Consumer<JsonNode> consumer) {
		try {

			logger.info("loading s3://{}/{} ... ", bucketName, key);
			AmazonS3Client s3 = scanner.getClient(AmazonS3ClientBuilder.class);
			S3Object obj = s3.getObject(bucketName, key);
			InputStream in = obj.getObjectContent();
			if (key.endsWith(".gz")) {
				in = new GZIPInputStream(in);
			}

			read(in, consumer);
			markState(bucketName, key, FileState.COMPLETE);
		} catch (IOException e) {
			markState(bucketName, key, FileState.FAILED);
			throw new RebarException(e);
		}

	}

	private java.util.List<String> fetchPendingFiles(String bucketName, String startAt) {

		Set<String> loadedKeys = loadRecentState();

		AmazonS3Client s3 = scanner.getClient(AmazonS3ClientBuilder.class);
		String account = this.scanner.getAccount();
		String region = this.scanner.getRegion().getName();
		ListObjectsV2Request r = new ListObjectsV2Request();
		r.withBucketName(bucketName);
		r.withStartAfter(startAt);

		String regex = ".*\\/(\\d+)\\/CloudTrail\\/(.*?)\\/.*";
		Pattern p = Pattern.compile(regex);
		ListObjectsV2Result result = null;

		List<String> results = Lists.newArrayList();
		do {
			result = s3.listObjectsV2(r);

			for (S3ObjectSummary it : result.getObjectSummaries()) {

				long lastMod = it.getLastModified().getTime();
				long ageMillis = System.currentTimeMillis() - lastMod;
				long ageMins = TimeUnit.MILLISECONDS.toMinutes(ageMillis);

				java.util.regex.Matcher m = p.matcher(it.getKey());
				if (m.matches()) {

					if (ageMins < TimeUnit.HOURS.toMinutes(HORIZON_HOURS)) {
						if (account.equals(m.group(1)) && region.equals(m.group(2))) {

							if (loadedKeys.contains(it.getKey())) {
								logger.debug("already loaded: {}", it.getKey());
							} else {
								markPending(bucketName, it.getKey(), lastMod);
								results.add(it.getKey());
							}

						}
					}

				}

			}

			r.setContinuationToken(result.getNextContinuationToken());

		} while (!Strings.isNullOrEmpty(r.getContinuationToken()));

		return results;
	}

	private Set<String> loadRecentState() {
		Set<String> keys = Sets.newHashSet();

		String region = scanner.getRegionName();

		String account = scanner.getAccount();

		scanner.getGraphDriver().cypher(
				"match (a:AwsCloudTrailIngest {account:{account},region:{region}}) where a.status<>'PENDING' return a")
				.param("account", account).param("region", region).forEach(it -> {

					String key = it.path("key").asText(null);

					keys.add(key);
				});

		return keys;
	}

	protected void markPending(String bucketName, String key, long ts) {

		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		scanner.getGraphDriver().cypher(
				"merge (a:AwsCloudTrailIngest {account:{account},region:{region},bucket:{bucket},key:{key}}) set a.fileTs={fileTs}, a.status={status}")
				.param("account", scanner.getAccount()).param("region", scanner.getRegion().getName()).param("key", key)
				.param("bucket", bucketName).param("fileTs", ts).param("status", FileState.PENDING.name()).exec();

	}

	protected void markState(String bucketName, String key, FileState state) {

		Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
		scanner.getGraphDriver().cypher(
				"merge (a:AwsCloudTrailIngest {account:{account},region:{region},bucket:{bucket},key:{key}}) on create set a.status={status} on match set a.status={status}")
				.param("account", scanner.getAccount()).param("region", scanner.getRegion().getName()).param("key", key)
				.param("bucket", bucketName).param("status", state.name()).exec();

	}

	protected void read(InputStream in, Consumer<JsonNode> consumer) {
		JsonParser parser = null;
		try {

			parser = Json.objectMapper().getFactory().createParser(in);

			JsonToken token = null;

			while ((token = parser.nextToken()) != null) {

				if (token == JsonToken.START_ARRAY && "Records".equals(parser.currentName())) {
					break;
				}
			}

			while (parser.nextToken() == JsonToken.START_OBJECT) {
				try {
					// read everything from this START_OBJECT to the matching END_OBJECT
					// and return it as a tree model ObjectNode
					ObjectNode node = Json.objectMapper().readTree(parser);
					consumer.accept(node);
				} catch (Exception e) {
					logger.warn("unexpected", e);
				}

			}
		} catch (IOException e) {
			throw new RebarException(e);
		} finally {
			if (parser != null) {
				try {
					parser.close();
				} catch (IOException e) {
					throw new RebarException(e);
				}
			}
		}
	}

	private void purgeIngestNodes(String bucketName) {
		scanner.getNeo4jDriver().cypher(
				"match (a:AwsCloudTrailIngest {account:{account},region:{region},bucket:{bucket}}) where a.fileTs<{cutoff} detach delete a")
				.param("account", scanner.getAccount()).param("region", scanner.getRegionName())
				.param("bucket", bucketName)
				.param("cutoff", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(HORIZON_HOURS)).exec();
	}

	private void process(String bucketName, Consumer<JsonNode> c) {

		long cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(HORIZON_HOURS);

		purgeIngestNodes(bucketName);

		String startWith = scanner.getNeo4jDriver().cypher(
				"match (a:AwsCloudTrailIngest {account:{account},region:{region}}) where a.fileTs>{cutoff} return a order by a.fileTs asc")
				.param("region", scanner.getRegionName()).param("account", scanner.getAccount())
				.param("bucket", bucketName).param("cutoff", cutoff).findFirst().orElse(MissingNode.getInstance())
				.path("key").asText(null);

		logger.info("starting with key: {}", startWith);
		List<String> pending = fetchPendingFiles(bucketName, startWith);
		pending.forEach(it -> {
			readS3(bucketName, it, c);
		});

	}

	public Observable<JsonNode> observableEvents(String bucketName) {

	
		ObservableOnSubscribe<JsonNode> handler = emitter -> {

			Consumer<JsonNode> n = node -> {
			
					emitter.onNext(node);

				
			};
			process(bucketName, n);
			emitter.onComplete();


		};

		Observable<JsonNode> x = Observable.create(handler);

		return x;
	}
}
