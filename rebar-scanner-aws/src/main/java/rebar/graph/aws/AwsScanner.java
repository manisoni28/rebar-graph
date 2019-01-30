/**
 * Copyright 2018-2019 Rob Schoening
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.SdkClientException;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import rebar.graph.core.GraphDB;
import rebar.graph.core.Scanner;
import rebar.graph.core.ScannerBuilder;
import rebar.graph.neo4j.GraphSchema;
import rebar.util.RebarException;

public final class AwsScanner extends Scanner {

	static Logger logger = LoggerFactory.getLogger(AwsScanner.class);
	List<Consumer<AwsClientBuilder<?, ?>>> configurers = Lists.newArrayList();

	Supplier<String> accountSupplier = Suppliers.memoize(this::doGetAccount);

	Regions region;

	Cache<String, AmazonWebServiceClient> clientCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES)
			.build();

	CloudWatchEvents eventDispatcher = new CloudWatchEvents(this);
	static Map<String, Class<? extends AwsEntityScanner>> typeMap = Maps.newHashMap();

	static {
		findEntityScanners();
	}

	protected AwsScanner(ScannerBuilder<? extends Scanner> builder) {
		super(builder);

		configurers = ImmutableList.copyOf(AwsScannerBuilder.class.cast(builder).configurers);

	}

	public String getAccount() {
		return accountSupplier.get();
	}

	
	public CloudWatchEvents cloudWatchEvents() {
		return eventDispatcher;
	}
	
	private String doGetAccount() {
		try {
			GetCallerIdentityResult result = newClientBuilder(AWSSecurityTokenServiceClientBuilder.class).build()
					.getCallerIdentity(new GetCallerIdentityRequest());
			return result.getAccount();
		} catch (SdkClientException e) {

		}

		GetCallerIdentityResult result = newClientBuilder(AWSSecurityTokenServiceClientBuilder.class)
				.withRegion(Regions.US_EAST_1).build().getCallerIdentity(new GetCallerIdentityRequest());
		return result.getAccount();
	}

	@SuppressWarnings("unchecked")
	<T extends AwsClientBuilder<?, ?>> T newClientBuilder(Class<T> x) {
		try {

			T clientBuilder = (T) x.getMethod("standard").invoke(null);
			configurers.forEach(configurer -> {
				configurer.accept(clientBuilder);
			});
			return (T) clientBuilder;
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RebarException(e);
		}
	}

	public <T extends AwsEntityScanner> T getEntityScanner(Class<T> clazz) {
		try {
			T t = clazz.newInstance();
			t.init(this);
			return t;

		} catch (IllegalAccessException | InstantiationException e) {
			throw new RebarException(e);
		}
	}

	public String getRegionName() {
		return getRegion().getName();
	}
	public Regions getRegion() {
		return region;
	}

	@SuppressWarnings("unchecked")
	public <T extends AmazonWebServiceClient> T getClient(Class<? extends AwsClientBuilder> builderClass) {

		Preconditions.checkArgument(getRegion() != null, "region must be set");
		String key = getAccount() + ":" + getRegion() + ":" + builderClass.getName();

		T client = (T) clientCache.getIfPresent(key);
		if (client == null) {

			logger.info("building new client for account={} region={} type={}", getAccount(), getRegion().getName(),
					builderClass.getName().replace("Builder", ""));
			AwsClientBuilder cb = newClientBuilder(builderClass);
			
			
			if (builderClass.equals(AmazonS3ClientBuilder.class)) {
				// There are very odd legacy rules for S3 and its bizarre-o semi-global behavior.
				// We need to force global bucket access mode if we are using a us-east-1 endpoint
				AmazonS3ClientBuilder s3b = (AmazonS3ClientBuilder) cb;
				if (region==Regions.US_EAST_1) {
					cb = s3b.enableForceGlobalBucketAccess();
				}
				else {
					cb = cb.withRegion(getRegion());
				}
			}
			else {
				cb = cb.withRegion(getRegion());
			}
			client = (T) cb.build();
			clientCache.put(key, client);
		}

		return client;
	}

	@Override
	protected void doScan() {
		getEntityScanner(AllEntityScannerGroup.class).scan();
	}

	<T extends AwsEntityScanner> T getEntityScannerForType(final String type) {

		String t = Strings.nullToEmpty(type).toLowerCase().trim();
		if (type.toLowerCase().startsWith("aws")) {
			t = t.substring(3);
		}

		Class<? extends AwsEntityScanner> es = typeMap.get(t);
		if (es == null) {
			throw new IllegalArgumentException("unsupported entity type: " + type);
		}
		return (T) getEntityScanner(es);

	}

	private static void findEntityScanners() {
		ScanResult result = new ClassGraph().enableClassInfo().whitelistPackages("rebar.graph.aws").scan();

		typeMap = Maps.newHashMap();
		result.getAllClasses().forEach(it -> {
			try {
				if (it.extendsSuperclass(AwsEntityScanner.class.getName())) {
					String n = it.getName().replace(AwsScanner.class.getPackage().getName() + ".", "")
							.replace("Scanner", "").toLowerCase();

					Class<? extends AwsEntityScanner> es = (Class<? extends AwsEntityScanner>) Class
							.forName(it.getName());
					typeMap.put(n, es);
				}
			} catch (ClassNotFoundException e) {
				logger.warn("problem", e);
			}
		});

	}

	@Override
	public void scan(String scannerType, String account, String region, String type, String id) {
		if (scannerType == null || (!scannerType.equals("aws"))) {
			logger.info("do not handle scanner type: {}", scannerType);
			return;
		}
		if (account == null || !account.equals(getAccount())) {
			logger.info("do not handle account: {}", account);
			return;
		}
		if (region == null || !region.equals(getRegion().getName())) {
			logger.info("do not handle region: {}", region);
			return;
		}
		try {
			AwsEntityScanner scanner = getEntityScannerForType(type);
			if (Strings.isNullOrEmpty(id)) {
				scanner.scan();
			} else {
				scanner.scan(id);
			}
		} catch (IllegalArgumentException e) {
			logger.warn("unsupported entity type: {}", type);
		}

	}

	public void applyConstraints() {
		GraphSchema s = getRebarGraph().getGraphDB().getNeo4jDriver().schema();
		s.createUniqueConstraint("AwsRegion", "name");
		s.createUniqueConstraint("AwsAvailabilityZone", "name");
		s.createUniqueConstraint("AwsAccount", "account");
		s.createUniqueConstraint("AwsSecurityGroup", "arn");
		s.createUniqueConstraint("AwsSubnet", "arn");
		s.createUniqueConstraint("AwsEc2Instance", "arn");
		s.createUniqueConstraint("AwsAmi", "arn");
		s.createUniqueConstraint("AwsLaunchConfig", "arn");
		s.createUniqueConstraint("AwsLaunchTemplate", "arn");
		s.createUniqueConstraint("AwsElb", "arn");
		s.createUniqueConstraint("AwsElbTargetGroup", "arn");
		s.createUniqueConstraint("AwsElbListener", "arn");
		s.createUniqueConstraint("AwsAsg", "arn");
		s.createUniqueConstraint("AwsEksCluster", "arn");
		s.createUniqueConstraint("AwsLambdaFunction", "arn");
		s.createUniqueConstraint("AwsVpc", "arn");
		
		s.createUniqueConstraint("AwsHostedZone", "id");
		s.createUniqueConstraint("AwsHostedZone", "arn");
		
		s.createUniqueConstraint("AwsSqsQueue", "url");
		s.createUniqueConstraint("AwsSqsQueue", "arn");
		s.createUniqueConstraint("AwsSnsTopic", "arn");
		s.createUniqueConstraint("AwsSnsSubscription", "arn");
		
		s.createUniqueConstraint("AwsS3Bucket", "arn");
		s.createUniqueConstraint("AwsS3Bucket", "name");
		
		s.createUniqueConstraint("AwsEmrCluster", "arn");
		
		s.createUniqueConstraint("AwsRouteTable","arn");
		s.createUniqueConstraint("AwsInternetGateway", "arn");
		s.createUniqueConstraint("AwsEgressOnlyInternetGateway", "arn");
	}

	public CloudTrailEvents cloudTrailEvents() {
		return new CloudTrailEvents(this);
	}
	public String toString() {
		return MoreObjects.toStringHelper(this).add("type", getScannerType()).add("account", getAccount())
				.add("region", getRegion().getName()).toString();
	}

}
