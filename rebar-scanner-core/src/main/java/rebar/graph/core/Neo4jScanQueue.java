package rebar.graph.core;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rebar.graph.neo4j.Neo4jDriver;

public class Neo4jScanQueue implements ScanQueue {

	String selfId = UUID.randomUUID().toString();
	Logger logger = LoggerFactory.getLogger(getClass());

	ScheduledExecutorService exeutor = Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder().setDaemon(true).setUncaughtExceptionHandler(this::handleException).build());
	Neo4jDriver neo4j;

	List<Subscription> subs = Lists.newArrayList();
	class Subscription {
		String type;
		String n0;
		String n1;
		Consumer<JsonNode> consumer;
	}
	public Neo4jScanQueue(Neo4jDriver driver) {

		this.neo4j = driver;

	}

	private void handleException(Thread t, Throwable e) {
		logger.warn("problem", e);
	}

	@Override
	public void submit(String type, String a, String b, String c, String d) {
		String id = UUID.randomUUID().toString();

		neo4j.cypher(
				"create (q:ScanQueueItem {id:{id},type:{type}}) set q.createTs=timestamp(),q.n0={n0},q.n1={n1},q.n2={n2},q.n3={n3} return q")
				.param("id", id).param("type", type).param("n0", a).param("n1", b).param("n2", c).param("n3", d).exec();

	}

	void purgeOldItems() {
		purgeOldItems(1, TimeUnit.MINUTES);
	}

	void purgeOldItems(int t, TimeUnit unit) {
		neo4j.cypher("match (q:ScanQueueItem) where q.createTs<(timestamp()-{age}) or (NOT exists(q.createTs)) detach delete q")
				.param("age", unit.toMillis(Math.abs(t))).exec();
	}

	void delete(String id) {
		neo4j.cypher("match (q:ScanQueueItem {id:{id},consumerId:{consumerId}}) detach delete q").param("id", id)
				.param("consumerId", selfId).exec();
	}
	boolean claim(JsonNode n) {
	
		return neo4j.cypher("match (q:ScanQueueItem {id:{id}}) where not exists(q.consumerId) set q.consumerId={selfId} return q").param("id",n.path("id").asText()).param("selfId", selfId).findFirst().isPresent();

	}
	boolean dispatch(JsonNode event) {
		return true;
	}
	
	List<String> getSubscriptionTypes() {
		
		List<String> tmp = Lists.newArrayList();
		subs.forEach(it->{
			tmp.add(it.type);
		});
		return tmp;
	}
	
	List<String> getSubscriptionAccounts() {
		List<String> tmp = Lists.newArrayList();
		subs.forEach(it->{
			tmp.add(it.n0);
		});
		return tmp;
	}
	public void start() {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					logger.debug("polling scan queue");
			
					neo4j.cypher(
							"match (q:ScanQueueItem) where q.type in {types} and q.n0 in {accounts} and (not exists (q.consumerId)) and q.createTs>timestamp()-60000 return q.id as id,q.createTs as createTs, q.type as type, q.n0 as n0")
							.param("selfId", selfId)
							.param("types", getSubscriptionTypes().toArray(new String[0]))
							.param("accounts", getSubscriptionAccounts().toArray(new String[0])).forEach(it -> {
								
								if (claim(it)) {
									logger.info("processing {}",it);
									delete(it.path("id").asText());
								}
								

							});

				} catch (Exception e) {
					logger.warn("", e);
				}
			}

		};
		logger.info("starting {}", this);
		exeutor.scheduleWithFixedDelay(r, 0, 5, TimeUnit.SECONDS);
		exeutor.scheduleWithFixedDelay(this::purgeOldItems, 0, 5, TimeUnit.MINUTES);
	}

	@Override
	public void subscribe(String type, String a, String b, Consumer<JsonNode> s) {
		Subscription sub = new Subscription();
		sub.type = type;
		sub.n0 = a;
		sub.n1=b;
		sub.consumer = s;
		
		subs.add(sub);
		
	}

}
