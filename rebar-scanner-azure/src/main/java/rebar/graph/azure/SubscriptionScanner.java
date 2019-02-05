package rebar.graph.azure;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.management.resources.Subscription;

import rebar.util.Json;

public class SubscriptionScanner extends AzureEntityScanner<Subscription> {



	@Override
	protected ObjectNode toJson(Subscription x) {
		ObjectNode n = Json.objectNode();
		
		n.put("urn", toUrn(x).get());
		n.put("subscriptionId", getSubscriptionId());
		n.put("graphEntityType", getEntityType().name());
		n.put("graphEntityGroup", "azure");

		
		n.put("displayName", x.displayName());
		n.put("key", x.key());
		n.put("state", x.state().name());
		return n;
	}

	protected SubscriptionScanner(AzureScanner scanner) {
		super(scanner);
		// TODO Auto-generated constructor stub
	}

	@Override
	public AzureEntityType getEntityType() {
		return AzureEntityType.AzureSubscription;
	}

	@Override
	protected void project(Subscription t) {
	
		ObjectNode n = toJson(t);
		
		getScanner().getRebarGraph().getGraphDB().nodes(getEntityType().name()).idKey("urn").properties(n).merge();
		
		
	}

	@Override
	protected void doScan() {
		

		Subscription sub = getClient().getCurrentSubscription();
		
		project(sub);
		
		
	}

	@Override
	public Optional<String> toUrn(Subscription t) {
		String urn = String.format("urn:azure::%s", t.subscriptionId());
		return Optional.of(urn);
	}
}
