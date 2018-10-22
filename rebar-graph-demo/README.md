# rebar-graph-demo

This is a containerized demo of rebar-graph with AWS and Kubernetes scanning enabled.  This image will start an instance of Neo4j and then 
run the rebar-graph code against AWS and/or Kubernetes.

## Running

To scan AWS:

```bash
docker run -it \
    -p 7474:7474 \
    -p 7687:7687 \
    -v $HOME/.aws:/var/lib/neo4j/.aws:ro \
    rebar/rebar-graph-demo
```

Point your browser to [http://localhost:7474](http://localhost:7474) and you can explore the graph of your AWS infrastructure.

To scan Kubernetes:

```bash
docker run -it \
    -p 7474:7474 \
    -p 7687:7687 \
    -v $HOME/.kube:/app/.kube:ro \
    rebar/rebar-graph-demo
```
Note: `$HOME/.kube` is mapped to `/app/.kube` because local kubernetes installations will have the url set to
http://localhost:6443 which will not resolve within the container.  The startup script will copy and re-write this to
http://host.docker.internal:6443
