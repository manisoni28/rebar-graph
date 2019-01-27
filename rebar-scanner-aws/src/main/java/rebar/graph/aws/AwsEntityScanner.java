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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.machinezoo.noexception.Exceptions;

import rebar.graph.core.GraphDB;
import rebar.graph.core.GraphDB.NodeOperation;
import rebar.graph.core.RelationshipBuilder;
import rebar.graph.core.RelationshipBuilder.Cardinality;
import rebar.graph.core.RelationshipBuilder.FromNode;
import rebar.util.Json;

public abstract class AwsEntityScanner<A extends Object> {

	Logger logger = LoggerFactory.getLogger(getClass());
	AwsScanner scanner;

	public static final String TAG_PREFIX = "tag_";
	protected static final Set<String> TAG_PREFIXES = ImmutableSet.of(TAG_PREFIX);
	private AwsEntityType entityType;

	boolean isEnabled() {
		return true;
	}
	protected void init(AwsScanner scanner) {
		this.scanner = scanner;

	}

	/**
	 * Convenience for initializing a NodeOperation with common boilerplate.
	 * 
	 * @param labelType
	 * @param attrName
	 * @param attrVal
	 * @return
	 */
	protected NodeOperation awsGraphNodes(String labelType) {
		return getGraphDB().nodes(labelType).id("region", getRegionName()).id("account", getAccount());
	}

	/**
	 * Convenience for initializing a NodeOperation with common boilerplate.
	 * 
	 * @param labelType
	 * @param attrName
	 * @param attrVal
	 * @return
	 */
	protected NodeOperation awsGraphNodes(String labelType, String attrName, String attrVal) {
		return awsGraphNodes(labelType).id(attrName, attrVal);
	}

	protected final void gc(String type, long cutoff) {
		gc(type, cutoff, new String[0]);
	}

	protected void gc(String type, long cutoff, String... attrs) {

		if (Strings.isNullOrEmpty(type)) {
			return;
		}
		Stopwatch sw = Stopwatch.createStarted();

		NodeOperation op = getGraphDB().nodes(type).whereAttributeLessThan(GraphDB.UPDATE_TS, cutoff).label(type)
				.id("region", getRegion().getName(), "account", getAccount());

		// extra attributes are useful for some entities like Elb/Nlb/Alb
		if (attrs != null) {
			for (int i = 0; i < attrs.length; i += 2) {
				op = op.id(attrs[i], attrs[i + 1]);
			}
		}
		AtomicInteger count = new AtomicInteger(0);
		op.match().forEach(it -> {
			Exceptions.log(logger).run(() -> {
				count.incrementAndGet();
				logger.info("running gc on {}", it.path(GraphDB.ENTITY_TYPE).asText());
				scan(it);
			});
		});

		if (count.get() > 0 || sw.elapsed(TimeUnit.MILLISECONDS) > 500L) {
			logger.info("gc for {} {} nodes took {}ms", count.get(), type, sw.elapsed(TimeUnit.MILLISECONDS));
		}

	}

	protected boolean isEntityOwner(JsonNode entity) {
		if (entity == null) {
			return false;
		}
		String account = entity.path("account").asText();
		String region = entity.path("region").asText();
		if (!account.equals(getAccount())) {
			return false;
		}

		if (!region.equals(getRegionName())) {
			return false;
		}
		return true;
	}

	protected void assertEntityOwner(JsonNode entity) {
		String account = entity.path("account").asText();
		String region = entity.path("region").asText();
		Preconditions.checkArgument(account.equals(getAccount()), "account for scanner and entity must match");
		Preconditions.checkArgument(region.equals(getRegion().getName()), "region for scanner and entity must match");
	}

	protected <T extends AmazonWebServiceClient> T getClient(Class<? extends AwsClientBuilder> xx) {
		return getAwsScanner().getClient(xx);
	}

	public GraphDB getGraphDB() {
		return getAwsScanner().getGraphDB();
	}

	public String getRegionName() {
		return getRegion().getName();
	}

	public Regions getRegion() {
		return getAwsScanner().getRegion();
	}

	public String getAccount() {
		return getAwsScanner().getAccount();
	}

	public AwsScanner getAwsScanner() {
		return scanner;
	}

	public final void scan() {
		if (!isEnabled()) {
			logger.info("skipping execution of {}",getClass().getSimpleName());
			return;
		}
		Stopwatch sw = Stopwatch.createStarted();
		String type = getEntityType() != AwsEntityType.UNKNOWN ? getEntityTypeName() : getClass().getSimpleName();
		logger.info("begin scan: {}", type);
		doScan();
		logger.info("end scan {} ({} ms)", type, sw.elapsed(TimeUnit.MILLISECONDS));
	}

	protected abstract void doScan();

	public abstract void scan(JsonNode entity);

	public abstract void scan(String id);

	public void tryExecute(Runnable r) {
		scanner.tryExecute(r);
	}

	public void maybeThrow(Exception t) {
		scanner.maybeThrow(t);
	}

	protected Optional<String> toArn(A awsObject) {
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	protected ObjectNode toJson(A awsObject) {

		Preconditions.checkNotNull(awsObject);
		ObjectNode n = Json.objectMapper().valueToTree(awsObject);
		n.put(GraphDB.ENTITY_TYPE, getEntityTypeName());
		n.put(GraphDB.ENTITY_GROUP, "aws");
		n.put("region", getRegionName());
		n.put("account", getAccount());

		toArn((A) awsObject).ifPresent(arn -> n.put("arn", arn));
		return n;
	}

	protected boolean isEntityType(JsonNode n) {
		return isEntityType(n, getEntityTypeName());
	}

	protected boolean isEntityType(JsonNode n, String type) {
		if (n == null) {
			return false;
		}
		return n.path(GraphDB.ENTITY_TYPE).asText().equals(type);
	}

	protected String generateStandardArn(String serviceType, String entity, String id) {
		return String.format("arn:aws:%s:%s:%s:%s/%s", serviceType, getRegionName(), getAccount(), entity, id);
	}

	public abstract AwsEntityType getEntityType();

	public String getEntityTypeName() {
		return getEntityType().name();
	}

	protected void mergeSubnetRelationships(String... args) {
		FromNode n = awsRelationships();

		if (args != null) {
			for (int i = 0; i < args.length; i += 2) {
				n = n.id(args[i], args[i + 1]);
			}
		}

		n.relationship("RESIDES_IN").on("subnets", "subnetId", Cardinality.MANY).to("AwsSubnet").merge();
	}

	protected void mergeSecurityGroupRelationships(String... args) {
		FromNode n = awsRelationships();

		if (args != null) {
			for (int i = 0; i < args.length; i += 2) {
				n = n.id(args[i], args[i + 1]);
			}
		}
		n.relationship("USES").on("securityGroups", "groupId", Cardinality.MANY).to("AwsSecurityGroup").merge();
	}

	public FromNode awsRelationships() {

		return new RelationshipBuilder().sourceIdAttribute("region", getRegionName())
				.sourceIdAttribute("account", getAccount()).targetIdAttribute("region", getRegionName())
				.targetIdAttribute("account", getAccount()).driver(getGraphDB().getNeo4jDriver())
				.from(getEntityTypeName());
	}

	protected void mergeAccountOwner(AwsEntityType t)  {
		if (t==AwsEntityType.AwsAccount) {
			return;
		}
		logger.info("merging owner AwsAccount({}) -> {}",getAccount(),t.name());
		getGraphDB().nodes(AwsEntityType.AwsAccount.name()).id("account", getAccount()).relationship("HAS").on("account", "account").to(t.name()).id("account", getAccount()).merge();;
	}
	protected void mergeAccountOwner() {
		mergeAccountOwner(getEntityType());
	}
	protected void mergeResidesInRegionRelationship(String... args) {
		
		FromNode n = awsRelationships();

		if (args != null) {
			for (int i = 0; i < args.length; i += 2) {
				n = n.id(args[i], args[i + 1]);
			}
		}
		n.relationship("RESIDES_IN").on("region", "region").to("AwsRegion").merge();
	}
}
