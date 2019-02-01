---
id: kubernetes
title: Kubernetes
sidebar_label: Kubernetes
---
# Kubernetes

The `kubernetes-scanner` will connect to your Kubernetes API server and extract all needed metadata.

You can run the `kubernetes-scanner` wherever you want, but it is easiest to run it as a pod in kubernetes itself.  You get the benefits of integrated
service account auth to your K8S control plane.  

## Launch Neo4j in Kubernetes Cluster

If you don't already have a Neo4j instance running, it is very easy to launch one in Kubernetes.  It won't have persisitent volumes, but
that's OK to start. 

The following will create a neo4j pod and a service that your scanner will use to connect to it:

```bash
kubectl create -f \
  https://raw.githubusercontent.com/rebar-cloud/rebar-graph/master/rebar-scanner-kubernetes/neo4j.yaml
```

Once the neo4j pod has started, you can expose the ports locally using `kubectl port-forward` so that you can use the neo4j
UI to interact with the database:

```bash
kubectl port-forward deployment/neo4j 7474:7474 7687:7687
```

Now you should be able to point your browser to [http://localhost:7474](http://localhost:7474) and use the neo4j console.

WARNING: This neo4j instance is for demo purposes only.  If the neo4j pod is killed all data will be lost.  

## Deploy Scanner

Now you should be able to schedule an instance of `rebar/kubernetes-scanner` inside your kubeernetes cluster.  In this example,
rebar will connect to the neo4j instance created above. 

That is, it will use K8S DNS service discovery to find the IP address it should use to connect to Neo4j.

```bash
kubectl create -f \
  https://raw.githubusercontent.com/rebar-cloud/rebar-graph/master/rebar-scanner-kubernetes/rebar.yaml
```

Note: If you want to connect to a different neo4j instance outside the kubernetes cluster, just set the `GRAPH_URL` environment property as you see fit. `GRAPH_USERNAME` and `GRAPH_PASSWORD` are supported options if your neo4j instance has auth enabled.  

## Try It

After a few seconds, `rebar/kubernetes-scanner` should have created a graph model of your cluster.

Run the following locally:

```bash
kubectl port-forward deployment/neo4j 7474:7474 7687:7687
```

And then point your browser to [http://localhost:7474](http://localhost:7474).

You should then be able to issue Cypher queries against your graph.

To a have quick look at the whole graph:

```
match (a) return a
```

You can start looking at pieces of the graph:

```
match (k:KubeCluster)--(n:KubeNamespace)--(d:KubeDeployment)--(rs:KubeReplicaSet)--(p:KubePod)
return
k,n,d,rs,p
```

