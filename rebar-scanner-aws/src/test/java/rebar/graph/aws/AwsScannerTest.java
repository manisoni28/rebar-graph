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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.github.classgraph.TypeSignature;
import rebar.util.Json;
import rebar.util.Sleep;

public class AwsScannerTest extends AwsIntegrationTest {

	@Override
	protected void beforeAll() {

		logger.info("deleting all aws entities...");
		deleteAllAwsEntities();
		getAwsScanner().scan();
	}

	public void testScannerType() {
		Assertions.assertThat(getAwsScanner().getScannerType()).isEqualTo("aws");
	}

	@Test
	public void testIt() {

		Assertions.assertThat(getAwsScanner()).isSameAs(getAwsScanner());

		Assertions.assertThat((Object) getAwsScanner().getClient(AmazonEC2ClientBuilder.class))
				.isSameAs(getAwsScanner().getClient(AmazonEC2ClientBuilder.class));

	}

	@Test
	public void testX() {

		getAwsScanner().scan("aws", getAwsScanner().getAccount(), getAwsScanner().getRegion().getName(), "ami", "aa");
	}

	void assertScanner(String name, Class type) {
		AwsEntityScanner s = getAwsScanner().getEntityScannerForType(name);
		Assertions.assertThat(s.getClass()).isSameAs(type);
	}

	@Test
	public void testEntityScanners() {
		assertScanner("securitygroup", SecurityGroupScanner.class);
		assertScanner("vpc", VpcScanner.class);
		assertScanner("ami", AmiScanner.class);

	}

	@Test
	public void testAll() {

	
		long ts = System.currentTimeMillis();
		getGraphDriver().cypher("match (a) where labels(a)[0]=~'Aws.*' return a,labels(a)[0] as label").stream()
				.forEach(x -> {
					String label = x.path("label").asText();
					JsonNode it = x.path("a");
					Assertions.assertThat(it.path("graphEntityGroup").asText())
							.as("%s should have graphEntityGroup=aws", it.toString())
							.isEqualTo(getAwsScanner().getScannerType());
					Assertions.assertThat(it.path("graphEntityType").asText())
							.as(label + " should have graphEntityType of " + it.path("label").asText())
							.isEqualTo(label);

					Assertions.assertThat(it.path("graphUpdateTs").asLong()).isCloseTo(System.currentTimeMillis(),
							Offset.offset(TimeUnit.MINUTES.toMillis(5)));

					Assertions.assertThat(it.path("graphEntityGroup").asText()).isEqualTo("aws");

					String arn = it.path("arn").asText();

					if (!Strings.isNullOrEmpty(arn)) {
					
						Assertions.assertThat(arn).startsWith("arn:aws:");
					}

					if (ImmutableList.of("AwsHostedZoneRecordSet", "AwsAccount", "AwsRegion", "AwsAvailabilityZone")
							.contains(it.path("graphEntityType").asText()) || it.path("graphEntityType").asText().startsWith("AwsIam")) {

					} else {

						Assertions.assertThat(it.has("region")).as("missing region attribute: %s", arn).isTrue();
						Assertions.assertThat(it.has("account")).as("missing account attribute: %s", arn).isTrue();

						if (!label.equals("AwsS3Bucket")) {
							// S3 is semi-global
							Assertions.assertThat(it.path("region").asText())
									.isEqualTo(getAwsScanner().getRegion().getName());
						}

						if (!it.path("graphEntityType").asText().equals("AwsAmi")) {
							// account on AMI doesn't necessarily match since AMIs are widely shared
							// cross-account
							Assertions.assertThat(it.path("account").asText()).as(arn)
									.isEqualTo(getAwsScanner().getAccount());
						}
					}
				});


		getGraphDriver().cypher(
				"match (a) where labels(a)[0]=~'Aws.*' return labels(a)[0] as label,a.graphEntityType as graphEntityType")
				.forEach(it -> {
					Assertions.assertThat(it.path("graphEntityType").asText()).isEqualTo(it.path("label").asText());
				});



		Set<String> uniqueIndexes = Sets.newHashSet();
		getGraphDriver().cypher("CALL db.indexes()").stream()
				.filter(p -> p.path("type").asText().equals("node_unique_property")).forEach(it -> {

					String label = it.path("tokenNames").path(0).asText();
					String property = it.path("properties").path(0).asText();

					uniqueIndexes.add(label + "." + property);

				});

		getGraphDriver().cypher(
				"match (a) where labels(a)[0]=~'Aws.*' and exists (a.arn) return distinct labels(a)[0] as label")
				.forEach(it -> {
					String n = it.path("label").asText() + ".arn";

					Assertions.assertThat(uniqueIndexes).as("should have unique index on %s", n).contains(n);
				});
		;
		uniqueIndexes.forEach(it->{
			logger.info("unique index: {}",it);
		});
		

	}

	@Test
	public void dumpMetrics() {
		getGraphDriver().metrics().getStatementStats().forEach(it->{
			Json.logger().info("cypher metrics",it.toJson());
		});
	}
	@Test
	public void testMaybeThrow() {
		getAwsScanner().maybeThrow(new RuntimeException("testing maybeThrow()"));
	}

	@Test
	public void testParallel() {

		getAwsScanner().getEntityScanner(ParallelScanner.class).withScanner(SqsScanner.class).scan();

	}
	
	@Test
	public void testValidRelationships() {
		// This is a bit monolithic, but it is helpful to keep track of all the
		// relationships
		List<String> validRelationships = Lists.newArrayList();
		validRelationships.add("AwsVpc RESIDES_IN AwsRegion");
		validRelationships.add("AwsAccount HAS AwsVpc");
		validRelationships.add("AwsAccount HAS AwsHostedZone");
		validRelationships.add("AwsRegion HAS AwsAvailabilityZone");
		validRelationships.add("AwsSubnet RESIDES_IN AwsAvailabilityZone");
		validRelationships.add("AwsEc2Instance USES AwsAmi");
		validRelationships.add("AwsEc2Instance USES AwsSecurityGroup");
		validRelationships.add("AwsEc2Instance RESIDES_IN AwsSubnet");
		validRelationships.add("AwsElb ATTACHED_TO AwsAsg");
		validRelationships.add("AwsElb RESIDES_IN AwsSubnet");
		validRelationships.add("AwsElb DISTRIBUTES_TRAFFIC_TO AwsEc2Instance");
		validRelationships.add("AwsElb DISTRIBUTES_TRAFFIC_TO AwsElbTargetGroup");
		validRelationships.add("AwsElb HAS AwsElbListener");
		validRelationships.add("AwsAsg USES AwsLaunchTemplate");
		validRelationships.add("AwsVpc HAS AwsSubnet");
		validRelationships.add("AwsVpc HAS AwsSecurityGroup");
		validRelationships.add("AwsAsg LAUNCHES_INSTANCES_IN AwsSubnet");
		validRelationships.add("AwsAsg USES AwsLaunchConfig");
		validRelationships.add("AwsAsg HAS AwsEc2Instance");

		validRelationships.add("AwsEksCluster RESIDES_IN AwsSubnet");
		validRelationships.add("AwsEksCluster USES AwsSecurityGroup");
		validRelationships.add("AwsHostedZone HAS AwsHostedZoneRecordSet");
		validRelationships.add("AwsAccount HAS AwsSnsTopic");
		validRelationships.add("AwsAccount HAS AwsS3Bucket");
		validRelationships.add("AwsAccount HAS AwsSqsQueue");
		validRelationships.add("AwsAccount HAS AwsEmrCluster");
		validRelationships.add("AwsAccount HAS AwsApiGatewayRestApi");
		validRelationships.add("AwsVpc HAS AwsVpcEndpoint");
		validRelationships.add("AwsVpc HAS AwsRouteTable");

		validRelationships.add("AwsInternetGateway ATTACHED_TO AwsVpc");
		validRelationships.add("AwsEgressOnlyInternetGateway ATTACHED_TO AwsVpc");
		validRelationships.add("AwsSubnet USES AwsRouteTable");

		validRelationships.add("AwsVpcEndpoint RESIDES_IN AwsSubnet");
		validRelationships.add("AwsVpcEndpoint USES AwsSecurityGroup");
		validRelationships.add("AwsAccount HAS AwsIamInstanceProfile");
		validRelationships.add("AwsAccount HAS AwsIamRole");
		validRelationships.add("AwsAccount HAS AwsIamUser");
		validRelationships.add("AwsAccount HAS AwsIamPolicy");
		validRelationships.add("AwsIamInstanceProfile USES AwsIamRole");
		validRelationships.add("AwsAccount HAS AwsSecurityGroup");
		validRelationships.add("AwsAccount HAS AwsLambdaFunction");
		validRelationships.add("AwsVpc HAS AwsElbTargetGroup");
		validRelationships.add("AwsAccount HAS AwsElbListener");
		getGraphDriver().cypher(
				"match (a)-[r]->(b) where labels(a)[0]=~'Aws.*' return a.graphEntityType as fromLabel,r,b.graphEntityType as toLabel,type(r) as relType")
			.stream().map(it->{
				return  (String) it.path("fromLabel").asText() + " " + it.path("relType").asText() + " "
						+ it.path("toLabel").asText();
			})
			.distinct()
				.forEach(rel->{
				
					Assertions.assertThat(validRelationships).contains(rel);
				});
	}
	
	@Test
	public void testArn() {
		
		Set<String> typesWithoutArn = getGraphDriver().cypher("match (a) where (NOT exists(a.arn)) and labels(a)[0]=~'Aws.*' return a.arn as arn,labels(a)[0] as label").stream().map(it->it.path("label").asText()).collect(Collectors.toSet());
		
		Assertions.assertThat(typesWithoutArn).contains(AwsEntityType.AwsAccount.name(),"AwsRegion","AwsAvailabilityZone");
		Set<String> validEntitiesWithoutArn = ImmutableSet.of(AwsEntityType.AwsApiGatewayRestApi.name(),
				AwsEntityType.AwsVpcEndpoint.name(), AwsEntityType.AwsRegion.name(),
				AwsEntityType.AwsAvailabilityZone.name(), AwsEntityType.AwsAccount.name(),
				AwsEntityType.AwsHostedZoneRecordSet.name());

		Assertions.assertThat(Sets.difference(typesWithoutArn, validEntitiesWithoutArn)).isEmpty();
		
	}
}
