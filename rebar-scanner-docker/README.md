#rebar-scanner-docker

Builds a graph model of docker infrastructure.


## usage


```
docker run \
-e GRAPH_URL=bolt://host.docker.internal:7687 \
-v /var/run/docker.sock:/var/run/docker.sock -it \
rebar/docker-scanner
```
