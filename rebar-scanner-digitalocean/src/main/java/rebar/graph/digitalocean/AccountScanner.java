package rebar.graph.digitalocean;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myjeeva.digitalocean.exception.DigitalOceanException;
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException;
import com.myjeeva.digitalocean.pojo.Account;

public class AccountScanner extends DigitalOceanEntityScanner<Account> {

	public AccountScanner(DigitalOceanScanner scanner) {
		super(scanner);
	}

	@Override
	protected ObjectNode toJson(Account account) {

		ObjectNode n = super.toJson(account);
		n.put("account", account.getUuid());
		n.put("email", account.getEmail());
		n.put("status", account.getStatus());
		n.put("statusMessage", account.getStatusMessage());
		n.put("ern",toErn(account).get());
		n.put("graphEntityType", DigitalOceanEntityType.DigitalOceanAccount.name());
		n.put("graphEntityGroup", "digitalocean");
		return n;
	}

	@Override
	protected void doScan() {

		try {
			Account account = getClient().getAccountInfo();
			adviseRateLimit(getEntityType(), account.getRateLimit());

			tryExecute(() -> project(account));

		} catch (DigitalOceanException | RequestUnsuccessfulException e) {
			maybeThrow(e);
		}
	}

	@Override
	protected void project(Account entity) {
		ObjectNode n = toJson(entity);
		digitalOceanNodes(getEntityType().name()).idKey("ern").properties(n).merge();

	}

	@Override
	public DigitalOceanEntityType getEntityType() {
		return DigitalOceanEntityType.DigitalOceanAccount;
	}

	@Override
	public void scan(String id) {
		scan();
		
	}

	@Override
	public void scan(JsonNode n) {
		scan();
		
	}

	@Override
	public Optional<String> toErn(Account t) {
		return Optional.ofNullable(String.format("ern:digitalocean::%s", t.getUuid()));
	}
}
