# Github

Odds are that your source code and infrastructure configuration is in GitHub.  But how do you relate your container images to actual repositories?

We've seen organizations with thousands of GitHub source repositories, which is all well and good, until it's 10pm and you're debugging an issue on some container and can't locate the source code easily.

The GitHub scanner helps to solve this problem by scanning your GitHub organizations and repositories so that you can relate your infrastructure entities to these sources of truth.

## Usage

The following will run the `github-scanner`:

```shell
docker run -it \
  -e GRAPH_URL=bolt://<neo4j-hostname>:7687 \
  -e GRAPH_USERNAME=<username> \
  -e GRAPH_PASSWORD=<password> \
  rebar/github-scanner
```

## Config

| Env Var | Description |  |
| ------- | -------|-----|
| `GITHUB_TOKEN` | OAuth Token | OAuth token that github-scanner will used to authenticate with GitHub |
| `GITHUB_USERNAME` | GitHub Username | Do not use this if you use `GITHUB_TOKEN` |
| `GITHUB_PASSWORD` | GitHub Password | Do not use this if you use `GITHUB_TOKEN` |
| `GITHUB_URL` | URL if you are using GitHub Enterprise | Not needed if you are using github.com |
| `GRAPH_URL` | Neo4j Database URL | bolt://NEO4J_HOSTNAME:7687 |
| `GRAPH_USERNAME` | Database Username | |
| `GRAPH_PASSWORD` | Database Password | |