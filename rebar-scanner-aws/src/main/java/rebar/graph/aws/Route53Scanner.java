package rebar.graph.aws;

import java.util.Optional;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.GetHostedZoneRequest;
import com.amazonaws.services.route53.model.GetHostedZoneResult;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesRequest;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.NoSuchHostedZoneException;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import rebar.util.Json;

public class Route53Scanner extends AwsEntityScanner<HostedZone> {

	void project(String hostedZone, ResourceRecordSet rs) {
		ObjectNode recordSet = toJson(hostedZone, rs);
		awsGraphNodes(AwsEntityType.AwsHostedZoneRecordSet.name()).idKey("hostedZoneId", "name").properties(recordSet)
				.merge();
	}

	void project(HostedZone z) {

		ObjectNode n = toJson(z);
		awsGraphNodes(AwsEntityType.AwsHostedZone.name()).idKey("arn").properties(n).merge();
	}

	protected void doMergeRelationships() {
		
		// Do not use awsGraphNodes here since hosted zones are not regional.  The selector predicates on region will 
		// cause things to not relate properly.
		getGraphDB().nodes(AwsEntityType.AwsHostedZone.name()).id("account", getAccount()).relationship("HAS")
				.on("id", "hostedZoneId").to(AwsEntityType.AwsHostedZoneRecordSet.name()).id("account", getAccount())
				.merge();

		
		getGraphDB().nodes(AwsEntityType.AwsAccount.name()).id("account", getAccount()).relationship("HAS")
				.on("account", "account").to(AwsEntityType.AwsHostedZone.name()).id("account", getAccount()).merge();
	}

	public void scanRecordSets(String z) {
		checkScanArgument(z);
		AmazonRoute53 r53 = getAwsScanner().getClient(AmazonRoute53ClientBuilder.class);
		ListResourceRecordSetsRequest request = new ListResourceRecordSetsRequest(z);
		do {
			ListResourceRecordSetsResult result = r53.listResourceRecordSets(request);
			if (result.isTruncated()) {
				request.setStartRecordIdentifier(result.getNextRecordIdentifier());
				request.setStartRecordName(result.getNextRecordName());
				request.setStartRecordType(result.getNextRecordType());
			} else {
				request.setStartRecordName(null);
			}
			result.getResourceRecordSets().forEach(it -> {
				project(z, it);
			});
		} while (!Strings.isNullOrEmpty(request.getStartRecordName()));
		doMergeRelationships();
	}

	@Override
	protected void doScan() {

		long ts = getGraphDB().getTimestamp();
		AmazonRoute53 r53 = getAwsScanner().getClient(AmazonRoute53ClientBuilder.class);

		ListHostedZonesRequest request = new ListHostedZonesRequest();

		do {
			ListHostedZonesResult result = r53.listHostedZones(request);

			result.getHostedZones().forEach(z -> {
				try {
					project(z);
					scanRecordSets(z.getId());
				} catch (RuntimeException e) {
					maybeThrow(e);
				}
			});
			if (result.isTruncated()) {
				request.setMarker(result.getMarker());
			} else {
				request.setMarker(null);
			}
		} while (!Strings.isNullOrEmpty(request.getMarker()));
		doMergeRelationships();
		gc(AwsEntityType.AwsHostedZone.name(), ts);
	}

	@Override
	public void doScan(JsonNode entity) {
		String account = entity.path("account").asText();
		String hostedZoneId = entity.path("id").asText();

		// Make sure that we own the account
		if (getAccount().equals(account)) {
			doScan(hostedZoneId);
		}
	}

	@Override
	public void doScan(String id) {
		checkScanArgument(id);
		try {
			AmazonRoute53 r53 = getAwsScanner().getClient(AmazonRoute53ClientBuilder.class);

			GetHostedZoneResult result = r53.getHostedZone(new GetHostedZoneRequest(id));

			HostedZone z = result.getHostedZone();

			project(z);
		} catch (NoSuchHostedZoneException e) {
			getGraphDB().nodes(AwsEntityType.AwsHostedZone.name()).id("account", getAccount()).id("id", id).delete();
		}

	}

	@Override
	public AwsEntityType getEntityType() {
		// TODO Auto-generated method stub
		return AwsEntityType.AwsHostedZone;
	}

	@Override
	protected Optional<String> toArn(HostedZone awsObject) {
		return Optional.of(String.format("arn:aws:route53:::hostedzone/%s", awsObject.getId()));
	}

	protected ObjectNode toJson(String hostedZone, ResourceRecordSet rs) {
		ObjectNode n = Json.objectMapper().valueToTree(rs);
		
		String hostname = rs.getName();
		while (hostname!=null && hostname.endsWith(".")) {
			hostname = hostname.substring(0,hostname.length()-1);
		}
		n.put("graphEntityType", AwsEntityType.AwsHostedZoneRecordSet.name());
		n.put("graphEntityGroup", "aws");
		n.put("hostname", hostname);
		n.put("account", getAccount());
		// no region
		n.put("hostedZoneId", hostedZone);
		ArrayNode values = Json.arrayNode();
		n.path("resourceRecords").forEach(it -> {
			String v = it.path("value").asText(null);
			values.add(v);
		});
		n.set("values", values);
		n.remove("resourceRecords");
		
		if (n.has("aliasTarget")) {
			n.set("aliasHostedZoneId",n.path("aliasTarget").path("hostedZoneId"));
			n.set("aliasDnsName",n.path("aliasTarget").path("dnsname"));
			n.set("aliasEvaluateTargetHealth",n.path("aliasTarget").path("evaluateTargetHealth"));
		}
		else {
			n.set("aliasHostedZoneId",null);
			n.set("aliasDnsName",null);
			n.set("aliasEvaluateTargetHealth", null);
		}
		n.remove("aliasTarget");
		
		// alias A records are kind of odd, because we don't have the A record IP so the values are missing.
		// But it is an A record, not a CNAME, record, so adding the alias record of the value seems wrong.
		
		return n;
	}

	@Override
	protected ObjectNode toJson(HostedZone awsObject) {

		ObjectNode n = super.toJson(awsObject);

		n.remove("callerReference");
		n.remove("region"); // route53 is global
		n.set("comment", n.path("config").path("comment"));
		n.set("privateZone", n.path("config").path("privateZone"));
		n.remove("config");

		return n;
	}

}
