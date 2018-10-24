
# rebar-graph-kubernetes

## Launch Neo4j in Kubernetes Cluster

You will need a neo4j instance to store the graph data.  Assuming you don't have one available, the following will create a 
neo4j database instance as a pod inside your cluster:

```bash
kubectl create -f \
  https://raw.githubusercontent.com/rebar-cloud/rebar-graph/master/rebar-graph-kubernetes/neo4j.yaml
```

Once the neo4j pod has started, you can expose the ports locally using `kubectl port-forward` so that you can use the neo4j
browser to interact with the database:

```bash
kubectl port-forward deployment/neo4j 7474:7474 7687:7687
```

Now you should be able to point your browser to [http://localhost:7474](http://localhost:7474) and use the neo4j console.

Note: This neo4j instance is for demo purposes only.  If the neo4j pod is restarted all data will be lost.  

## Deploy rebar-graph-kubernetes

Now you should be able to schedule an instance of `rebar-graph-kubernetes` inside your kubeernetes cluster.  In this example,
rebar will connect to the neo4j instance created above. 

```bash
kubectl create -f \
  https://raw.githubusercontent.com/rebar-cloud/rebar-graph/master/rebar-graph-kubernetes/rebar.yaml
```

Note: If you want to connect to a different neo4j instance, just set the `GRAPH_URL` environment property as you see fit. `GRAPH_USERNAME` and `GRAPH_PASSWORD` are supported options if your neo4j instance has auth enabled.

## Try It

After a few seconds, `rebar-graph-kubernetes` should have created a graph model of your cluster.

Run the following locally:

```bash
kubectl port-forward deployment/neo4j 7474:7474 7687:7687
```

And then point your browser to [http://localhost:7474](http://localhost:7474).

You should then be able to issue Cypher queries against your graph.

To a quick look at the whole graph:

```
match (a) return a
```


