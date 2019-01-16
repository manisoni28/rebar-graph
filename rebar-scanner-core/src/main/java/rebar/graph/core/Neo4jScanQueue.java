package rebar.graph.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rebar.graph.neo4j.GraphDriver;

public class Neo4jScanQueue implements ScanQueue {

	String selfId = UUID.randomUUID().toString();
	Logger logger = LoggerFactory.getLogger(getClass());

	ScheduledExecutorService exeutor = Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder().setDaemon(true).setUncaughtExceptionHandler(this::handleException).build());
	GraphDriver neo4j;

	List<Subscription> subs = Lists.newCopyOnWriteArrayList();

	public static class Subscription {
		String type;
		String n0;
		String n1;
		Consumer<JsonNode> consumer;

		public String toString() {
			return MoreObjects.toStringHelper(this).add("type", type).add("n0", n0).add("n1", n1).toString();
		}

		public boolean match(JsonNode n) {
			return match(n.path("type").asText(null), n.path("n0").asText(null), n.path("n1").asText(null));
		}

		public boolean match(String type, String a, String b) {

			if (!this.type.equals(Strings.nullToEmpty(type))) {
				return false;
			}

			if (n0==null) {
				return true;
			}
			if (Strings.isNullOrEmpty(a) && Strings.isNullOrEmpty(n0)) {
				return true;
			}
			if (!Strings.nullToEmpty(n0).equals(Strings.nullToEmpty(a))) {
				return false;
			}
			if (n1==null) {
				return true;
			}
			if (Strings.isNullOrEmpty(b) && Strings.isNullOrEmpty(n1)) {
				return true;
			}
			if (!Strings.nullToEmpty(b).equals(Strings.nullToEmpty(n1))) {
				return false;
			}
			return true;
		}
	}

	public Neo4jScanQueue(GraphDriver driver) {

		this.neo4j = driver;

	}

	private void handleException(Thread t, Throwable e) {
		logger.warn("problem", e);
	}

	@Override
	public void submit(String type, String a, String ...args) {
		String id = UUID.randomUUID().toString();

		Map<String,String> data = Maps.newHashMap();
		
		data.put("n0", a);
		if (args!=null) {
			for (int i=0; i<args.length; i++) {
				data.put("n"+(i+1), args[i]);
			}
		}
		neo4j.cypher(
				"create (q:ScanQueueItem {id:{id},type:{type}}) set q.createTs=timestamp(),q+={data} return q")
				.param("id", id).param("type", type).param("data",data).exec();
		
	
		

	}

	void purgeOldItems() {
		purgeOldItems(1, TimeUnit.MINUTES);
	}

	void purgeOldItems(int t, TimeUnit unit) {
		neo4j.cypher(
				"match (q:ScanQueueItem) where q.createTs<(timestamp()-{age}) or (NOT exists(q.createTs)) detach delete q")
				.param("age", unit.toMillis(Math.abs(t))).exec();
	}

	void deleteQueueItem(String id) {
		logger.info("deleting ScanQueueItem: {}",id);
		neo4j.cypher("match (q:ScanQueueItem {id:{id},consumerId:{consumerId}}) detach delete q").param("id", id)
				.param("consumerId", selfId).exec();
	}

	boolean claimQueueItem(JsonNode n) {

		if (subs.stream().anyMatch(sub -> sub.match(n))) {

			return neo4j.cypher(
					"match (q:ScanQueueItem {id:{id}}) where not exists(q.consumerId) set q.consumerId={selfId} return q")
					.param("id", n.path("id").asText()).param("selfId", selfId).findFirst().isPresent();
		} else {
			return false;
		}
	}

	void processQueueItem(JsonNode event) {
		logger.info("processing {}", event);

		subs.forEach(sub -> {
			try {
				if (sub.match(event)) {
					sub.consumer.accept(event);
				}
			} catch (Exception e) {
				logger.warn("unhandled exception while processing subscription " + sub, e);
			}
		});

	}

	List<String> getSubscriptionTypes() {

		List<String> tmp = Lists.newArrayList();
		subs.forEach(it -> {
			tmp.add(it.type);
		});
		return tmp;
	}

	List<String> getSubscriptionAccounts() {
		List<String> tmp = Lists.newArrayList();
		subs.forEach(it -> {
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
							"match (q:ScanQueueItem) where q.type in {types} and q.n0 in {accounts} and (not exists (q.consumerId)) and q.createTs>timestamp()-60000 return q.id as id,q.createTs as createTs, q.type as type, q.n0 as n0, q.n1 as n1, q.n2 as n2, q.n3 as n3")
							.param("selfId", selfId).param("types", getSubscriptionTypes().toArray(new String[0]))
							.param("accounts", getSubscriptionAccounts().toArray(new String[0])).forEach(it -> {

								if (claimQueueItem(it)) {
									try {
										processQueueItem(it);
									} finally {
										deleteQueueItem(it.path("id").asText());
									}
								}
								else {
									logger.info("unclaimed: {}",it);
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
	public void subscribe(Consumer<JsonNode> consumer, String type, String a, String b) {
		Subscription sub = new Subscription();
		sub.type = type;
		sub.n0 = a;
		sub.n1 = b;
		sub.consumer = consumer;

		subs.add(sub);

	}

	public void unsubscribe(String type, String a, String b) {

		List<Subscription> toBeRemoved = Lists.newArrayList();
		subs.forEach(it -> {
			if (it.type.equals(type) && ((it.n0 == null && a == null) || it.n0.equals(a))
					&& ((it.n1 == null && b == null) || it.n1.equals(b))) {
				toBeRemoved.add(it);
			}
		});
		subs.removeAll(toBeRemoved);
	}

}
