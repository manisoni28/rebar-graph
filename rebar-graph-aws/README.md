# rebar-graph-aws

Builds graph model of AWS infrastructure.

## Try It 

You will need a neo4j graph database instance to store the data that rebar will gather from AWS.

Assuming you don't have a neo4j instance you will want to start one by running the following:

```bash
docker run \
    -it --rm \
    --publish=7474:7474 --publish=7687:7687 \
    --env=NEO4J_ACCEPT_LICENSE_AGREEMENT=yes \
    --env=NEO4J_AUTH=none \
    --name=neo4j \
    neo4j:3.4
```

After Neo4j has started, you can access the console in your browser: [http://localhost:7474](http://localhost:7474)

Now you can run `rebar-graph-aws`:

```
docker run -it \
  -e GRAPH_URL=bolt://host.docker.internal:7687 \
  -v $HOME/.aws:/root/.aws:ro \
  rebar/rebar-graph-aws
```

For this to work, you need valid AWS credentials in `$HOME/.aws`.  Note also that `host.docker.internal` only works on Docker for Mac/Windows.  For Linux, you would need to provide the host IP in the `GRAPH_URL` environment variable.

Once this has run, you can enter a Cypher query to see the graph:

The following will display the entire graph:

```
match (a) return a;
```


| Type | Status | Notes |
|------|-------|-------|
| AwsRegion | ✅ |  |
| AwsAvailabilityZone | ✅ |  |
| AwsAccount | ✅ | |
| AwsVpc | ✅ | |
| AwsSubnet | ✅  | |
| AwsSecurityGroup | ✅  | |
| AwsAsg | ✅ |  |
| AwsElb | ✅ |  |
| AwsEc2Instance | ✅ |  |
| AwsLaunchConfig | ✅ | |
| AwsLaunchTemplate | ✅ | |
| AwsAmi | ✅ | |
| AwsS3Bucket   | ⛔ | |
| AwsEksCluster | ⛔ | |
| AwsRdsCluster | ⛔ | |
| AwsRoute53RecordSet | ⛔ | |
| AwsRoute53HostedZone   | ⛔ | |
| AwsAlb | ⛔ | |
| AwsNlb | ⛔ | |
| AwsTargetGroup| ⛔ | |
| AwsSqsQueue | ⛔ | |
| AwsSnsTopic | ⛔ | |
| AwsSnsSubscription | ⛔ | |
| AwsSes | ⛔ | |
| AwsGlue | ⛔ | |
| AwsKinesisStream | ⛔ | |
| AwsRouteTable | ⛔ | |
| AwsVpnGateway   | ⛔ | |
| AwsVpcPeeringConnection   | ⛔ | |
| AwsVpcEndpoint   | ⛔ | |
| AwsInternetGateway | ⛔ | |
| AwsIamUser   | ⛔ | |
| AwsIamInlinePolicy   | ⛔ | |
| AwsIamRole | ⛔ | |
| AwsIamPolicy | ⛔ | |
| AwsIamManagedPolicy| ⛔ | |
| AwsEc2NetworkInterface| ⛔ | |
| AwsEcrRepository| ⛔ | |
| AwsDynamoDbTable| ⛔ | |


