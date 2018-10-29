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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import rebar.graph.core.GraphDB;
import rebar.graph.core.Scanner;
import rebar.graph.core.ScannerBuilder;
import rebar.util.RebarException;

public final class AwsScanner extends Scanner {

	Logger logger = LoggerFactory.getLogger(AwsScanner.class);
	List<Consumer<AwsClientBuilder<?, ?>>> configurers = Lists.newArrayList();

	Supplier<String> accountSupplier = Suppliers.memoize(this::doGetAccount);

	Regions region;

	Cache<String, AmazonWebServiceClient> clientCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES)
			.build();

	protected AwsScanner(ScannerBuilder<? extends Scanner> builder) {
		super(builder);

		configurers = ImmutableList.copyOf(AwsScannerBuilder.class.cast(builder).configurers);

	}

	public String getAccount() {
		return accountSupplier.get();
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

	public <T extends AbstractEntityScanner> T getScanner(Class<T> clazz) {
		try {
			Constructor<T> ctor = clazz.getConstructor(AwsScanner.class);
			return (T) ctor.newInstance(this);
		
		}
		catch (NoSuchMethodException | IllegalAccessException | InstantiationException| InvocationTargetException e) {
			throw new RebarException(e);
		}
	}


	public Regions getRegion() {
		return region;
	}

	public GraphDB getGraphDB() {
		return getRebarGraph().getGraphDB();
	}

	
	@SuppressWarnings("unchecked")
	public <T extends AmazonWebServiceClient> T getClient(Class<? extends AwsClientBuilder> builderClass) {

		Preconditions.checkArgument(getRegion() != null, "region must be set");
		String key = getAccount() + ":" + getRegion() + ":" + builderClass.getName();

		T client = (T) clientCache.getIfPresent(key);
		if (client == null) {

			logger.info("building new client for account={} region={} type={}",getAccount(),getRegion().getName(),builderClass.getName().replace("Builder", ""));
			client = (T) newClientBuilder(builderClass).withRegion(getRegion()).build();
			clientCache.put(key, client);
		}
		
		return client;
	}


	@Override
	public void doScan() {
		getScanner(AllEntityScanner.class);
	}
	public void maybeThrow(Exception e) {
		if (isFailOnError()) {
			if (e instanceof RuntimeException) {
				throw RuntimeException.class.cast(e);
			}
			else {
				throw new RebarException("problem",e);
			}
		}
		else {
			logger.warn("problem",e);
		}
	}

}
