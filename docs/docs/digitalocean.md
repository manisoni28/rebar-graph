# Digital Ocean

The Digital Ocean scanner, like all the rebar scanners, is intended to be deployed as a Docker container.  You can run it on your laptop, on an isolated host, or in a kubernetes cluster.  Your pleasure.

The scanner, running in the contiainer, will connect to Digital Ocean and interrogate it on a periodic basis.

## Running

Launch the Digital Ocean scanner container with the following invocation:

```shell
docker run -it \
    -e GRAPH_URL=bolt://<neo4j-host>:7687 \
    -e GRAPH_USERNAME=<username> \
    -e GRAPH_PASSWORD=<password> \
    -e DIGITALOCEAN_ACCESS_TOKEN=<token> \
    rebar/digitalocean-scanner
```

If you are running Neo4j in docker on your laptop, you can use `GRAPH_URL=host.docker.internal:7687` and it will just work.  

If you do not have auth enabled in Neo4j, you can omit `GRAPH_USERNAME` and `GRAPH_PASSWORD`.

If you would rather not pass your access token via environment argument, you can volume-map your `~/.config/doctl/config.yaml` file into the container with: `-v $HOME/.config:/rebar/.config`.

For instance, the following does what I need for my environment:

```shell
docker run -it \
-e GRAPH_URL=bolt://host.docker.internal:7687 \
-v $HOME/.config:/rebar/.config \
rebar/digitalocean-scanner
```
## Configuration Parameters

| Env Var | Description | Example |
|------|------|------|
| `DIGITALOCEAN_ACCESS_TOKEN` | Your Access Token |
| `GRAPH_URL` | Neo4j Database URL | bolt://myserver:7687 |
| `GRAPH_USERNAME` | Database Username | |
| `GRAPH_PASSWORD` | Database Password

## Data Model

The following are the currently supported entities. 

| Node Type | Description | 
|-----------|---------|
| `DigitalOceanAccount` | Your DigitalOcean Account |
| `DigitalOceanRegion` | The various Digital Ocean regions |
| `DigitalOceanDroplet` | Your Droplets |

We will be adding support for images, load balancers, etc. as need/opportunity arises.


