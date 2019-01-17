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
package rebar.graph.kubernetes;

import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.batch.CronJob;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.Watcher;
import rebar.graph.core.GraphDB.NodeOperation;
import rebar.graph.core.GraphDB;
import rebar.graph.core.Scanner;
import rebar.util.Json;

public class KubeScanner extends Scanner {

	static Logger logger = LoggerFactory.getLogger(KubeScanner.class);
	KubernetesClient client;
	String clusterId;

	protected static final String ANNOTATION_PREFIX = "annotation_";
	protected static final String LABEL_PREFIX = "label_";

	protected static final String KUBE_CLUSTER_LABEL = "KubeCluster";
	protected static final String KUBE_CONTAINER_LABEL = "KubeContainer";

	protected static final String CLUSTER_ID = "clusterId";
	protected static final String NAME = "name";
	protected static final String NAMESPACE = "namespace";
	protected static final String UID = "uid";
	protected static final String HAS = "HAS";

	protected static final Set<String> TAG_PREFIXES = ImmutableSet.of(ANNOTATION_PREFIX, LABEL_PREFIX);

	public String getClusterId() {
		Preconditions.checkNotNull(clusterId);
		return clusterId;
	}

	public KubeScanner(KubeScannerBuilder builder) {
		super(builder);

	}

	public void scanCluster() {

		URL url = getKubernetesClient().getMasterUrl();

		VersionInfo version = getKubernetesClient().getVersion();

		getKubernetesClient().pods().inAnyNamespace().list();
		ObjectNode n = Json.objectMapper().valueToTree(version);

		n.put("masterUrl", url.toExternalForm());
		n.put(CLUSTER_ID, getClusterId());
		n.remove("data");
		n.put(GraphDB.ENTITY_GROUP, "kubernetes");
		n.put(GraphDB.ENTITY_TYPE, "KubeCluster");

		Optional<JsonNode> existing = getRebarGraph().getGraphDB().nodes(KUBE_CLUSTER_LABEL)
				.id(CLUSTER_ID, getClusterId()).match().findFirst();
		if ((!existing.isPresent()) || (!existing.get().has("name"))) {
			n.put("name", clusterId);
		}

		getRebarGraph().getGraphDB().nodes(KUBE_CLUSTER_LABEL).id(CLUSTER_ID, getClusterId()).properties(n).merge();

	}

	protected void projectService(Service svc, String namespace, String name) {
		if (deleteIfNecessary(svc, Service.class, namespace, name)) {
			return;
		}

		ObjectNode n = toJson(svc);

		ServiceSpec spec = svc.getSpec();

		ObjectNode x = Json.objectMapper().valueToTree(spec);
		x.fields().forEachRemaining(it -> {
			n.set(it.getKey(), it.getValue());
		});

		n.path("ports").forEach(port -> {

			String portName = "clusterIpPort_" + port.path("protocol").asText() + "_"
					+ port.path("targetPort").asText();
			n.put(portName, port.path("port").asInt());
			portName = "clusterIpPort_" + port.path("protocol").asText() + "_" + port.path(NAME).asText();
			n.put(portName, port.path("port").asInt());

			if (port.path("nodePort").isNumber()) {
				portName = "nodePort_" + port.path("protocol").asText() + "_" + port.path("targetPort").asText();
				n.put(portName, port.path("nodePort").asInt());
				portName = "nodePort_" + port.path("protocol").asText() + "_" + port.path(NAME).asText();
				n.put(portName, port.path("nodePort").asInt());
			}
		});

		n.set("sessionAffinityClientIPTimeoutSeconds",
				n.path("sessionAffinityConfig").path("clientIP").path("timeoutSeconds"));
		n.remove("sessionAffinityConfig");
		n.path("selector").fields().forEachRemaining(it -> {
			n.put("selector_" + it.getKey(), it.getValue().asText());
		});

		n.remove("selector");
		n.remove("spec");
		n.remove("ports");
		n.remove("status");

		getRebarGraph().getGraphDB().nodes(toEntityType(Service.class)).withTagPrefixes(TAG_PREFIXES)
				.idKey(CLUSTER_ID, NAMESPACE, NAME).properties(n).merge();

		ensureNamespaceRelationships(Service.class);

	}

	protected String toEntityType(HasMetadata md) {
		Preconditions.checkNotNull(md);
		return "Kube" + md.getKind();
	}

	protected String toEntityType(JsonNode n) {
		Preconditions.checkNotNull(n);

		String val = n.path("kind").asText(null);
		if (Strings.isNullOrEmpty(val)) {
			val = n.path(GraphDB.ENTITY_TYPE).asText(null);
			Preconditions.checkArgument(!Strings.isNullOrEmpty(val), "kind or entityType must be set");
			return val;
		}
		return "Kube" + val;
	}

	protected String toEntityType(Class<? extends HasMetadata> md) {
		return "Kube" + md.getName().substring(md.getName().lastIndexOf(".") + 1);

	}

	DeploymentWatcher deploymentWatcher;
	PodWatcher podWatcher;
	ReplicaSetWatcher replicaSetWatcher;
	NodeWatcher nodeWatcher;
	EndpointsWatcher endpointsWatcher;
	ServiceWatcher serviceWatcher;

	private String watchName(Watcher w) {
		List<String> x = Splitter.on("$").splitToList(w.getClass().getName());
		return x.get(x.size() - 1);
	}

	public synchronized void watchEvents() {

		Stopwatch allStopwatch = Stopwatch.createStarted();
		try {
			if (this.deploymentWatcher == null) {
				Stopwatch sw = Stopwatch.createStarted();

				DeploymentWatcher w = new DeploymentWatcher();

				getKubernetesClient().apps().deployments().watch(w);
				this.deploymentWatcher = w;

				logger.info("registered {} in {} ms", watchName(w), sw.elapsed(TimeUnit.MILLISECONDS));
			}

			if (this.podWatcher == null) {
				Stopwatch sw = Stopwatch.createStarted();
				PodWatcher w = new PodWatcher();

				getKubernetesClient().pods().watch(w);
				this.podWatcher = w;
				logger.info("registered {} in {} ms", watchName(w), sw.elapsed(TimeUnit.MILLISECONDS));
			}

			if (this.replicaSetWatcher == null) {
				Stopwatch sw = Stopwatch.createStarted();
				ReplicaSetWatcher w = new ReplicaSetWatcher();
				getKubernetesClient().apps().replicaSets().watch(w);
				this.replicaSetWatcher = w;
				logger.info("registered {} in {} ms", watchName(w), sw.elapsed(TimeUnit.MILLISECONDS));
			}

			if (this.nodeWatcher == null) {
				Stopwatch sw = Stopwatch.createStarted();
				NodeWatcher w = new NodeWatcher();
				getKubernetesClient().nodes().watch(w);
				this.nodeWatcher = w;
				logger.info("registered {} in {} ms", watchName(w), sw.elapsed(TimeUnit.MILLISECONDS));
			}

			if (this.serviceWatcher == null) {
				Stopwatch sw = Stopwatch.createStarted();
				ServiceWatcher w = new ServiceWatcher();
				getKubernetesClient().services().watch(w);
				this.serviceWatcher = w;
				logger.info("registered {} in {} ms", watchName(w), sw.elapsed(TimeUnit.MILLISECONDS));
			}

			// There seems to be a bug in the fabric8 client or Docker's kubernetes
			// distribution
			// that causes more than 5 watches to timeout.
			boolean extendedWatchEnabled = false;
			if (extendedWatchEnabled) {

				if (this.endpointsWatcher == null) {
					Stopwatch sw = Stopwatch.createStarted();
					EndpointsWatcher w = new EndpointsWatcher();
					getKubernetesClient().endpoints().watch(w);
					this.endpointsWatcher = w;
					logger.info("registered {} in {} ms", watchName(w), sw.elapsed(TimeUnit.MILLISECONDS));
				}

				logger.info("registering DaemonSet watch...");
				getKubernetesClient().apps().daemonSets().watch(new DaemonSetWatcher());

				logger.info("registering Batch watch...");
				getKubernetesClient().batch().cronjobs().watch(new CronJobWatcher());

				logger.info("registering Secrets watch...");
				getKubernetesClient().secrets().watch(new SecretWatcher());

				logger.info("registering ConfigMap watch...");
				getKubernetesClient().configMaps().watch(new ConfigMapWatcher());
			}
		} finally {
			logger.info("Watch registration complete ({} ms)", allStopwatch.elapsed(TimeUnit.MILLISECONDS));
		}
	}

	abstract class GenericWatcher<T> implements Watcher<T> {
		void fixThreadName() {
			// The thread name is really annoying
			Thread t = Thread.currentThread();
			String threadName = t.getName();
			
			if (threadName.startsWith("OkHttp")) {
				
				t.setName("evt-"+getClusterId()+"-"+t.getId());
			}
		}
		@Override
		public void eventReceived(Action action, T resource) {

			fixThreadName();
			HasMetadata md = (HasMetadata) resource;
			logger.info("event {} {}", action, KubeScanner.toString(md));

			if (action.name().equals("DELETED")) {
				deleteEntity(md);
				return;
			}

			scan(md.getKind(), md.getMetadata().getNamespace(), md.getMetadata().getName());

		}

		@Override
		public void onClose(KubernetesClientException cause) {
			logger.warn("onClose", cause);

		}
	}

	class CronJobWatcher extends GenericWatcher<CronJob> {

	}

	class SecretWatcher extends GenericWatcher<Secret> {

	}

	class ConfigMapWatcher extends GenericWatcher<ConfigMap> {

	}

	class ServiceWatcher extends GenericWatcher<Service> {

	}

	class DeploymentWatcher extends GenericWatcher<Deployment> {

	}

	class ReplicaSetWatcher extends GenericWatcher<ReplicaSet> {

	}

	class DaemonSetWatcher extends GenericWatcher<DaemonSet> {

	}

	class NodeWatcher extends GenericWatcher<Node> {

	}

	class EndpointsWatcher extends GenericWatcher<Endpoints> {

		@Override
		public void eventReceived(Action action, Endpoints resource) {
			if (resource.getMetadata().getNamespace().equals("kube-system")) {
				return;
			}
			super.eventReceived(action, resource);
		}

	}

	class PodWatcher extends GenericWatcher<Pod> {

	}

	private void ensureNamespaceRelationships(Class<? extends HasMetadata> clazz) {

		getRebarGraph().getGraphDB().nodes(toEntityType(Namespace.class)).id(CLUSTER_ID, getClusterId())
				.relationship(HAS).on(NAME, NAMESPACE).to(toEntityType(clazz)).id(CLUSTER_ID, getClusterId()).merge();

	}

	public void scanServices() {

		long ts = getRebarGraph().getGraphDB().getTimestamp();
		getKubernetesClient().services().inAnyNamespace().list().getItems().forEach(it -> {

			projectService(it, it.getMetadata().getNamespace(), it.getMetadata().getName());
		});

		scanEndpoints();
		gc(Service.class, ts);
		ensureNamespaceRelationships(Service.class);

	}

	ObjectNode toJson(HasMetadata hmd) {

		ObjectNode x = Json.objectMapper().valueToTree(hmd);
		x.put(GraphDB.ENTITY_GROUP, "kubernetes");
		x.put(GraphDB.ENTITY_TYPE, toEntityType(hmd));
		x.remove("data");
		x.remove("metadata");

		Optional.ofNullable(hmd.getMetadata()).ifPresent(m -> {
			x.put(CLUSTER_ID, getClusterId());
			x.put("apiVersion", hmd.getApiVersion()).put("kind", hmd.getKind()).put("clusterName", m.getClusterName())
					.put("resourceVersion", m.getResourceVersion()).put("creationTimestamp", m.getCreationTimestamp())
					.put("deletionGracePeriodSeconds", m.getDeletionGracePeriodSeconds())
					.put("deletionTimestamp", m.getDeletionTimestamp()).put("generateName", m.getGenerateName())
					.put("generation", m.getGeneration()).put(NAME, m.getName()).put(NAMESPACE, m.getNamespace())
					.put("selfLink", m.getSelfLink()).put(UID, m.getUid());

			parseDateTime(m.getDeletionTimestamp()).ifPresent(t -> {
				x.put("deletionTs", t.toEpochMilli());
			});
			parseDateTime(m.getCreationTimestamp()).ifPresent(t -> {
				x.put("creationTs", t.toEpochMilli());
			});
			Optional.ofNullable(m.getAnnotations()).ifPresent(a -> {
				a.forEach((k, v) -> {
					x.put(ANNOTATION_PREFIX + k, v);
				});
			});
			Optional.ofNullable(m.getLabels()).ifPresent(l -> {
				l.forEach((k, v) -> {

					x.put(LABEL_PREFIX + k, v);
				});
			});

			x.put("deploymentUid", (String) null);
			x.put("replicaSetUid", (String) null);
			x.put("daemonSetUid", (String) null);

			m.getOwnerReferences().forEach(ref -> {
				String refType = toOwnerRefProperty(ref.getKind());

				x.put(refType, ref.getUid());

			});

		});

		x.put(GraphDB.ENTITY_TYPE, toEntityType(x));

		return x;

	}

	protected void projectNode(Node node, String name) {
		if (node == null && !Strings.isNullOrEmpty(name)) {
			getRebarGraph().getGraphDB().nodes(toEntityType(Node.class)).id("clusterId", getClusterId()).id(NAME, name)
					.delete();
			return;
		}

		ObjectNode nx = toJson(node);
		nx.path("nodeInfo").fields().forEachRemaining(it -> {
			nx.set(it.getKey(), it.getValue());
		});

		nx.set("externalID", nx.path("spec").path("externalID"));
		nx.remove("spec");

		JsonNode allocatable = nx.path("status").path("allocatable");
		allocatable.fields().forEachRemaining(it -> {
			nx.set("allocatable_" + it.getKey(), it.getValue());
		});
		JsonNode capactity = nx.path("status").path("capacity");
		capactity.fields().forEachRemaining(it -> {
			nx.set("capacity_" + it.getKey(), it.getValue());
		});
		nx.remove("status");

		getRebarGraph().getGraphDB().nodes(toEntityType(Node.class)).withTagPrefixes(TAG_PREFIXES)
				.id("clusterId", getClusterId()).id(NAME, name).properties(nx).merge();

		getRebarGraph().getGraphDB().nodes(KUBE_CLUSTER_LABEL).id(CLUSTER_ID, getClusterId()).relationship(HAS)
				.to(toEntityType(Node.class)).id(NAME, node.getMetadata().getName()).merge();

	}

	protected void projectNamespace(Namespace n, String name) {

		if (n == null && !Strings.isNullOrEmpty(name)) {
			getRebarGraph().getGraphDB().nodes(toEntityType(Namespace.class)).id(NAME, name)
					.id(CLUSTER_ID, getClusterId()).delete();
			return;
		}

		ObjectNode nx = toJson(n);
		
		nx.set("finalizers", nx.path("spec").path("finalizers"));
		nx.remove("spec");
		nx.set("phase", nx.path("status").path("phase"));
		nx.remove("status");
		getRebarGraph().getGraphDB().nodes(toEntityType(Namespace.class)).idKey(CLUSTER_ID, NAME)
				.withTagPrefixes(TAG_PREFIXES).properties(nx).merge();
		getRebarGraph().getGraphDB().nodes(KUBE_CLUSTER_LABEL).id(CLUSTER_ID, getClusterId()).relationship(HAS)
				.to(toEntityType(Namespace.class)).id(UID, n.getMetadata().getUid()).merge();

	}

	protected void projectContainerSpec(Pod p) {
		long ts = getRebarGraph().getGraphDB().getTimestamp();
		p.getSpec().getContainers().forEach(it -> {
			ObjectNode cs = Json.objectMapper().valueToTree(it);

			cs.put("podUid", p.getMetadata().getUid());
			cs.put("containerSpecId", p.getMetadata().getUid() + "-" + cs.path("name").asText());
			cs.put(GraphDB.ENTITY_GROUP, "kubernetes");
			cs.put(GraphDB.ENTITY_TYPE, "KubeContainerSpec");
			cs.put("clusterId", getClusterId());
			cs.put(GraphDB.UPDATE_TS, ts + 1);
			getRebarGraph().getGraphDB().nodes("KubeContainerSpec").withTagPrefixes(TAG_PREFIXES)
					.idKey("containerSpecId").properties(cs).merge();

		});
		getRebarGraph().getGraphDB().nodes("KubeContainerSpec").whereAttributeLessThan(GraphDB.UPDATE_TS, ts)
				.id("podUid", p.getMetadata().getUid()).delete();

		getRebarGraph().getGraphDB().nodes("KubePod").id("uid", p.getMetadata().getUid()).relationship("DEFINED_BY")
				.on("uid", "podUid").to("KubeContainerSpec").id("podUid", p.getMetadata().getUid()).merge();

	}

	protected void projectContainerStatus(Pod p, List<ContainerStatus> cs) {
		List<String> containerIdList = cs.stream().map(x -> x.getContainerID()).collect(Collectors.toList());

		long ts = getRebarGraph().getGraphDB().getTimestamp();
		cs.forEach(it -> {
			ObjectNode n = Json.objectMapper().valueToTree(it);
			n.put(GraphDB.ENTITY_GROUP, "kubernetes");
			n.put(GraphDB.ENTITY_TYPE, "KubeContainer");
			n.put("startedAt", n.path("state").path("running").path("startedAt").asText());
			n.remove("lastState");
			n.remove("state");
			n.put("podUid", p.getMetadata().getUid());
			String containerID = it.getContainerID();
			if (!Strings.isNullOrEmpty(containerID)) {
				// containers in the process of starting will not have containerID set
				getRebarGraph().getGraphDB().nodes(KUBE_CONTAINER_LABEL).id(CLUSTER_ID, getClusterId())
						.id("containerID", it.getContainerID()).properties(n).merge();
			}
		});

		getRebarGraph().getGraphDB().nodes(toEntityType(Pod.class)).id(CLUSTER_ID, getClusterId())
				.id(UID, p.getMetadata().getUid()).relationship(HAS).on(UID, "podUid").to(KUBE_CONTAINER_LABEL)
				.id(CLUSTER_ID, getClusterId()).merge();

		getRebarGraph().getGraphDB().nodes("KubeContainer").id("podUid", p.getMetadata().getUid())
				.relationship("DEFINED_BY").to("KubeContainerSpec").id("podUid", p.getMetadata().getUid()).merge();

		ObjectNode arg = Json.objectNode();
		arg.put(CLUSTER_ID, getClusterId());
		arg.put(GraphDB.ENTITY_TYPE, KUBE_CONTAINER_LABEL);
		arg.put(GraphDB.ENTITY_GROUP, "kubernetes");
		arg.put(GraphDB.UPDATE_TS, ts);
		arg.put("podUid", p.getMetadata().getUid());
		arg.set("containers", Json.objectMapper().valueToTree(containerIdList));
		execGraphOperation(RemoveStalePodContainersOperation.class, arg);
	}

	private void ensurePodNodeRelationships() {
		getRebarGraph().getGraphDB().nodes(toEntityType(Pod.class)).id("clusterId", getClusterId())
				.relationship("RUNS_ON").on("nodeName", NAME).to(toEntityType(Node.class))
				.id("clusterId", getClusterId()).merge();
	}

	protected void projectPod(Pod p, String ns, String name) {

		if (deleteIfNecessary(p, Pod.class, ns, name)) {
			return;
		}

		ObjectNode nx = toJson(p);

		PodStatus podStatus = p.getStatus();

		String podUid = p.getMetadata().getUid();
		nx.put("hostIP", podStatus.getHostIP());
		nx.put("phase", podStatus.getPhase());
		nx.put("podIP", podStatus.getPodIP());
		nx.put("qosClass", podStatus.getQosClass());
		nx.put("startTime", podStatus.getStartTime());

		List<ContainerStatus> csList = Lists.newArrayList();
		podStatus.getContainerStatuses().forEach(cs -> {
			csList.add(cs);
		});

		nx.path("spec").fields().forEachRemaining(it -> {
			if (it.getValue() == null || !it.getValue().isContainerNode()) {
				nx.set(it.getKey(), it.getValue());
			}
		});

		nx.remove("spec"); // container spec is very complicated
		// TODO list of conditions in status object that we might want to map
		nx.remove("status");

		getRebarGraph().getGraphDB().nodes(nx.path(GraphDB.ENTITY_TYPE).asText()).withTagPrefixes(TAG_PREFIXES)
				.idKey(CLUSTER_ID, NAMESPACE, NAME).properties(nx).merge();
		projectContainerSpec(p);
		projectContainerStatus(p, csList);

		execGraphOperation(MergePodParentRelationshipsOperation.class, nx);

	}

	public static String toString(HasMetadata md) {

		if (md == null) {
			return MoreObjects.toStringHelper("null").toString();
		}
		return MoreObjects.toStringHelper("Kube" + md.getKind()).add(NAMESPACE, md.getMetadata().getNamespace())
				.add(NAME, md.getMetadata().getName()).add(UID, md.getMetadata().getUid()).toString();
	}

	protected void projectDeployment(Deployment d, String ns, String name) {

		if (deleteIfNecessary(d, Deployment.class, ns, name)) {

			return;
		}
		ObjectNode n = toJson(d);

		DeploymentStatus ds = d.getStatus();
		if (ds != null) {
			n.put("availableReplicas", ds.getAvailableReplicas());
			n.put("collisionCount", ds.getCollisionCount());
			n.put("observedGeneration", ds.getObservedGeneration());
			n.put("readyReplicas", ds.getCollisionCount());
			n.put("replicas", ds.getReplicas());
			n.put("unavailableReplicas", ds.getCollisionCount());
			n.put("updatedReplicas", ds.getUpdatedReplicas());
		}
		n.remove("status");

		n.remove("spec");

		getRebarGraph().getGraphDB().nodes(toEntityType(Deployment.class)).withTagPrefixes(TAG_PREFIXES)
				.idKey(CLUSTER_ID, NAMESPACE, NAME).properties(n).merge();

	}

	protected void checkNamespaceAndName(String ns, String name) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(ns), "namespace must be set");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "namespace must be set");
	}

	public void scanService(String ns, String name) {
		checkNamespaceAndName(ns, name);
		projectService(getKubernetesClient().services().inNamespace(ns).withName(name).get(), ns, name);
		ensureNamespaceRelationships(Service.class);
		scanEndpoints(ns, name);

	}

	public void scanReplicaSet(String ns, String name) {
		checkNamespaceAndName(ns, name);
		projectReplicaSet(getKubernetesClient().apps().replicaSets().inNamespace(ns).withName(name).get(), ns, name);

		ensureNamespaceRelationships(ReplicaSet.class);
		ensureOwnerReferences(Deployment.class, ReplicaSet.class);
		ensureOwnerReferences(ReplicaSet.class, Pod.class);
	}

	private void scanEndpoints(String ns, String name) {
		checkNamespaceAndName(ns, name);
		projectEndpoints(getKubernetesClient().endpoints().inNamespace(ns).withName(name).get(), ns, name);
	}

	public void scanDaemonSet(String ns, String name) {
		checkNamespaceAndName(ns, name);
		projectDaemonSet(getKubernetesClient().apps().daemonSets().inNamespace(ns).withName(name).get(), ns, name);
		ensureNamespaceRelationships(DaemonSet.class);
		ensureOwnerReferences(DaemonSet.class, Pod.class);
	}

	public void scanDeployment(String ns, String name) {
		checkNamespaceAndName(ns, name);
		projectDeployment(getKubernetesClient().apps().deployments().inNamespace(ns).withName(name).get(), ns, name);
		ensureNamespaceRelationships(Deployment.class);
		ensureOwnerReferences(Deployment.class, ReplicaSet.class);
	}

	public void scanDeployments() {
		long ts = getRebarGraph().getGraphDB().getTimestamp();
		getKubernetesClient().apps().deployments().inAnyNamespace().list().getItems().forEach(it -> {
			projectDeployment(it, it.getMetadata().getNamespace(), it.getMetadata().getName());
		});
		gc(Deployment.class, ts);
		ensureNamespaceRelationships(Deployment.class);
		ensureOwnerReferences(Deployment.class, ReplicaSet.class);
	}

	public void scanPod(String namespace, String name) {

		Pod pod = getKubernetesClient().pods().inNamespace(namespace).withName(name).get();
		projectPod(pod, namespace, name);
		ensureNamespaceRelationships(Pod.class);
		ensureOwnerReferences(ReplicaSet.class, Pod.class);
		ensureOwnerReferences(DaemonSet.class, Pod.class);
	}

	public void scanPods() {

		long ts = getRebarGraph().getGraphDB().getTimestamp();
		getKubernetesClient().pods().inAnyNamespace().list().getItems().forEach(it -> {
			projectPod(it, it.getMetadata().getNamespace(), it.getMetadata().getName());
		});
		gc(Pod.class, ts);

		ensureNamespaceRelationships(Pod.class);
		ensureOwnerReferences(ReplicaSet.class, Pod.class);
		ensurePodNodeRelationships();
	}

	protected void deleteNodeByUid(String type, String uid) {
		getRebarGraph().getGraphDB().nodes(type).id(CLUSTER_ID, getClusterId()).id(UID, uid).delete();
	}

	protected void deleteNodeByName(String type, String ns, String name) {
		getRebarGraph().getGraphDB().nodes(type).id(CLUSTER_ID, getClusterId()).id(NAMESPACE, ns).id(NAME, name)
				.delete();
	}

	private boolean deleteIfNecessary(HasMetadata md, Class<? extends HasMetadata> cz, String ns, String name) {

		String entityType = toEntityType(cz);
		if (md == null && (!Strings.isNullOrEmpty(ns) && (!Strings.isNullOrEmpty(name)))) {
			getRebarGraph().getGraphDB().nodes(entityType).id(CLUSTER_ID, getClusterId()).id(NAMESPACE, ns)
					.id(NAME, name).delete();
			return true;
		}
		return false;
	}

	String prefixKey(String prefix, String key) {
		return prefix + "_" + key;

	}

	protected void projectReplicaSet(ReplicaSet rs, String ns, String name) {

		if (deleteIfNecessary(rs, ReplicaSet.class, ns, name)) {
			return;
		}

		ObjectNode n = toJson(rs);

		n.path("status").fields().forEachRemaining(it -> {
			n.set(prefixKey("status", it.getKey()), it.getValue());
		});

		n.remove("status");
		n.remove("spec"); // spec is very complicated
		getRebarGraph().getGraphDB().nodes(toEntityType(ReplicaSet.class)).withTagPrefixes(TAG_PREFIXES)
				.id(NAMESPACE, ns).id(NAME, name).properties(n).merge();

		ensureNamespaceRelationships(ReplicaSet.class);
		ensureOwnerReferences(Deployment.class, ReplicaSet.class);
		ensureOwnerReferences(ReplicaSet.class, Pod.class);

	}

	protected void projectEndpoints(Endpoints endpoints, String ns, String name) {
		if (deleteIfNecessary(endpoints, Endpoints.class, ns, name)) {
			return;
		}

		ObjectNode n = Json.objectNode();
		n.put(GraphDB.ENTITY_GROUP, "kubernetes");
		n.put(GraphDB.ENTITY_TYPE, toEntityType(Service.class));
		n.put(NAMESPACE, ns);
		n.put(NAME, name);
		n.put(CLUSTER_ID, getClusterId());

		ArrayNode podRef = n.arrayNode();
		n.set("podRef", podRef);
		endpoints.getSubsets().forEach(s -> {
			s.getAddresses().forEach(ea -> {
				ObjectReference ref = ea.getTargetRef();
				if (ref != null) {
					if (ref.getKind().equals("Pod")) {
						podRef.add(ref.getUid());
					}
				}
			});
		});

		getRebarGraph().getGraphDB().nodes(toEntityType(Service.class)).id(CLUSTER_ID, getClusterId()).id(NAMESPACE, ns)
				.id(NAME, name).properties(n).match(); // we use match here so that we don't create phantom services
		ensureNamespaceRelationships(Service.class);
		execGraphOperation(MergeServiceEndpointsOperation.class, n);
	}

	private void scanEndpoints() {
		getKubernetesClient().endpoints().inAnyNamespace().list().getItems().forEach(it -> {

			tryExecute(() -> projectEndpoints(it, it.getMetadata().getNamespace(), it.getMetadata().getName()));

		});

	}

	public void scanReplicaSets() {

		long ts = getRebarGraph().getGraphDB().getTimestamp();
		getKubernetesClient().apps().replicaSets().inAnyNamespace().list().getItems().forEach(it -> {

			tryExecute(() -> projectReplicaSet(it, it.getMetadata().getNamespace(), it.getMetadata().getName()));

		});
		gc(ReplicaSet.class, ts);
		ensureNamespaceRelationships(ReplicaSet.class);
		ensureOwnerReferences(ReplicaSet.class, Pod.class);
	}

	public void scanNamespace(String ns) {
		Namespace n = getKubernetesClient().namespaces().withName(ns).get();

		tryExecute(() -> projectNamespace(n, ns));

	}

	private void gc(Class<? extends HasMetadata> md, long ts) {

		String type = toEntityType(md);
		getRebarGraph().getGraphDB().nodes(type).id(CLUSTER_ID, getClusterId())
				.whereAttributeLessThan(GraphDB.UPDATE_TS, ts).match().forEach(it -> {
					logger.info("running gc on {} namespace={} name={}", type, it.path(NAMESPACE).asText(),
							it.path(NAME).asText());

					scan(type, it.path(NAMESPACE).asText(), it.path(NAME).asText());
				});

	}

	public void scanNode(String name) {
		Node n = getKubernetesClient().nodes().withName(name).get();
		projectNode(n, name);
	}

	public void scanNodes() {
		long ts = getRebarGraph().getGraphDB().getTimestamp();
		getKubernetesClient().nodes().list().getItems().forEach(it -> {

			tryExecute(() -> projectNode(it, it.getMetadata().getName()));

		});
		ensurePodNodeRelationships();
		ensureEc2Relationships();
		gc(Node.class, ts);
	}

	public void scanNamespaces() {

		long ts = getRebarGraph().getGraphDB().getTimestamp();
		getKubernetesClient().namespaces().list().getItems().forEach(it -> {

			tryExecute(() -> projectNamespace(it, null));

		});

		gc(Namespace.class, ts);
	}

	protected static String toOwnerRefProperty(String kind) {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, kind.replace("Kube", "")) + "Uid";
	}

	protected static String ownerRefProperty(HasMetadata md) {
		return toOwnerRefProperty(md.getKind());
	}

	protected void ensureOwnerReferences(Class<? extends HasMetadata> ownerClass,
			Class<? extends HasMetadata> childClass) {

		String ownerType = toEntityType(ownerClass);
		String childType = toEntityType(childClass);

		String ownerTypeKey = toOwnerRefProperty(toEntityType(ownerClass));

		getRebarGraph().getGraphDB().nodes(ownerType).id(CLUSTER_ID, getClusterId()).relationship(HAS)
				.on(UID, ownerTypeKey).to(childType).id(CLUSTER_ID, getClusterId()).merge();

		ObjectNode arg = Json.objectNode();
		arg.put("ownerType", ownerType);
		arg.put("childType", childType);
		arg.put(CLUSTER_ID, getClusterId());
		execGraphOperation(RemoveStaleRelationshipsOperation.class, arg);

	}

	protected void projectDaemonSet(DaemonSet ds, String ns, String name) {
		if (deleteIfNecessary(ds, DaemonSet.class, ns, name)) {
			return;
		}

		ObjectNode n = toJson(ds);

		n.remove("status");
		n.remove("spec");
		getRebarGraph().getGraphDB().nodes(toEntityType(ds.getClass())).withTagPrefixes(TAG_PREFIXES)
				.idKey(CLUSTER_ID, NAMESPACE, NAME).properties(n).merge();

	}

	public void scanDaemonSets() {

		long ts = getRebarGraph().getGraphDB().getTimestamp();
		getKubernetesClient().apps().daemonSets().list().getItems().forEach(it -> {

			tryExecute(() -> projectDaemonSet(it, it.getMetadata().getNamespace(), it.getMetadata().getName()));

		});
		gc(DaemonSet.class, ts);
		ensureNamespaceRelationships(DaemonSet.class);
		ensureOwnerReferences(DaemonSet.class, Pod.class);
	}

	protected static Optional<Instant> parseDateTime(String input) {

		if (Strings.isNullOrEmpty(Strings.nullToEmpty(input).trim())) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(input.trim())));
		} catch (DateTimeParseException e) {
			logger.warn("could not parse: {}", e.toString());
			return Optional.empty();
		}

	}

	public KubernetesClient getKubernetesClient() {

		Preconditions.checkNotNull(client);
		return client;
	}

	public void scan(JsonNode n) {
		scan(n.path(GraphDB.ENTITY_TYPE).asText(), n.path(NAMESPACE).asText(), n.path(NAME).asText());
	}

	public void scan(String typeToScan, String namespace, String name) {

		logger.info("scan {} namespace={} name={}", typeToScan, namespace, name);
		String type = Strings.nullToEmpty(typeToScan).trim().toLowerCase();
		if (type.startsWith("kube")) {
			type = type.substring(4);
		}

		if (type.equals("cluster")) {
			scanCluster();
		} else if (type.equals(NAMESPACE.toLowerCase())) {
			scanNamespace(name);
		} else if (type.equals("deployment")) {
			scanDeployment(namespace, name);
		} else if (type.equals("replicaset")) {
			scanReplicaSet(namespace, name);
		} else if (type.equals("service")) {
			scanService(namespace, name);
		} else if (type.equals("daemonset")) {
			scanDaemonSet(namespace, name);
		} else if (type.equals("pod")) {
			scanPod(namespace, name);
		} else if (type.equals("node")) {
			scanNode(name);
		} else {

			logger.warn("unknown type to scan: {}", typeToScan);
		}
	}

	private void deleteEntity(HasMetadata md) {
		if (md == null) {
			return;
		}
		String entityType = "Kube" + md.getKind();
		String namespace = md.getMetadata().getNamespace();
		String name = md.getMetadata().getName();

		NodeOperation op = getRebarGraph().getGraphDB().nodes(entityType).id("clusterId", getClusterId()).id(NAME,
				name);

		if (!Strings.isNullOrEmpty(namespace)) {
			// don't specify namespace for non-namespace objects
			op = op.id(NAMESPACE, namespace);
		}
		op.delete();
	}

	@Override
	public void doScan() {
		scanCluster();
		scanNodes();
		scanNamespaces();
		scanDeployments();
		scanPods(); // should come before ReplicSet,Service,DaemonSet
		scanReplicaSets();
		scanDaemonSets();
		scanServices();

	}

	public void applyConstraints() {

		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubeCluster", "name");
		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubeCluster", "clusterId");
		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubeNode", "uid");
		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubePod", "uid");
		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubeDeployment", "uid");
		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubeReplicaSet", "uid");
		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubeDaemonSet", "uid");
		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubeService", "uid");
		getRebarGraph().getGraphDB().schema().ensureUniqueIndex("KubeNamespace", "uid");

	}

	@Override
	public void scan(String scannerType, String cluster, String namespace, String type, String name) {
		if (scannerType == null || (!scannerType.equals("kubernetes"))) {
			return;
		}
		if (cluster == null || !cluster.equals(getClusterId())) {
			return;
		}
		if (Strings.isNullOrEmpty(type)) {
			return;
		}

		String t = type.replace("Kube", "").toLowerCase();
		if (Strings.isNullOrEmpty(name)) {
			if (t.equals("cluster")) {
				scanCluster();
			} else if (t.equals("node")) {
				scanNodes();
			} else if (t.equals("pod")) {

			} else if (t.equals("deployment")) {

			} else if (t.equals("replicaset")) {

			} else if (t.equals("daemonset")) {

			} else if (t.equals("service")) {

			} else if (t.equals("namespace")) {

			}
			else {
				logger.warn("cannot process kube type: {}",type);
			}
		}
		else {
			
			scan(type, namespace, name);
			
		}
	}


	private void ensureEc2Relationships() {
		getGraphDB().nodes("KubeNode").relationship("PROVIDED_BY").on("label_kubernetes.io/hostname", "privateDnsName").to("AwsEc2Instance").merge();
	}
}
