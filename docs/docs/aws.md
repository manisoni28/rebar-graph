---
id: aws
title: AWS
sidebar_label: AWS
---



# Overview

The aws-scanner is deployed as a docker container.  It connects to the AWS control plane and queries it to extract metadata.
It then normalizes and projects that data into Neo4j, the graph database.

# Getting Started 

You will need a neo4j graph database instance to store the data that rebar will gather from AWS.  If you don't have neo4j installed already, skip down to [Install Neo4j](#install-neo4j).

## Start AWS Scanner Image

Now you can run `aws-scanner`:

```
docker run -it \
  -e GRAPH_URL=bolt://host.docker.internal:7687 \
  -v $HOME/.aws:/rebar/.aws:ro \
  rebar/aws-scanner
```

*Note: if you need to set username/password, they should be set with the environment variables: `GRAPH_USERNAME` and `GRAPH_PASSWORD`.*

For this to work, you need valid AWS credentials in `$HOME/.aws`.  Note also that `host.docker.internal` only works on Docker for Mac/Windows.  For Linux, you would need to provide the host IP in the `GRAPH_URL` environment variables.

Once this has run, you can enter a Cypher query in the Neo4j console to see the graph:

The following will display the entire graph

```
match (a) return a;
```

## Install Neo4j

### Docker Install
The easiest way to get started with Neo4j is by launching it in a docker container.  The following will start a container and expose ports `7474`(HTTP) and `7687` (BOLT database protocol) on your local machine.

```bash
docker run \
    -it --rm \
    --publish=7474:7474 --publish=7687:7687 \
    --env=NEO4J_ACCEPT_LICENSE_AGREEMENT=yes \
    --env=NEO4J_AUTH=none \
    --name=neo4j \
    neo4j:3.5
```
Once it has started, you can point your browser to http://localhost:7474 to use the web UI.

Note: It is not recommended that you disable auth in a deployed environment.  However, disabling auth makes local development easier. 

### Native Install

If you don't want to run neo4j in docker, you can [download](https://neo4j.com/download-center/#releases) and install it for your platform. 

The neo4j download site is here: https://neo4j.com/download-center/#releases


## Configuration

The following options can be passed as environment variables. 


| Env Variable   | Required? | Description           | Example                |
| -------------- | --------- | --------------------- | ---------------------- |
| GRAPH_URL      | Required  | URL of Graph Database | `bolt://myserver:7687` |
| GRAPH_USERNAME | -         | Database username     |                        |
| GRAPH_PASSWORD | -         | Database password     |                        |
| AWS_REGIONS    | -        | Comma-seaparated list of regions to be scanned.  If omitted, only the "current" region will be scanned. | `us-east`,`us-west-2` |



## Supported Types

| Type | Scanner | |
|------|-----|------|
| AwsAccount |[AccountScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/AccountScanner.java) | |
| AwsAmi | [AmiScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/AmiScanner.java)| |
| AwsApiGatewayRestApi | [ApiGatewayRestApiScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/ApiGatewayRestApiScanner.java) | |
| AwsAsg | [AsgScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/AsgScanner.java) | |
| AwsAvailabilityZone | [AvailabilityZoneScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/AvailabilityZoneScanner.java) |  |
| AwsCacheCluster | [ElastiCacheScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/ElastiCacheScanner.java) | |
| AwsCacheClusterNode |[ElastiCacheScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/ElastiCacheScanner.java)   | |
| AwsEc2Instance |[Ec2InstanceScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/Ec2InstanceScanner.java)   | |
| AwsEgressOnlyInternetGateway | [EgressOnlyInternetGatewayScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/EgressOnlyInternetGatewayScanner.java)  | |
| AwsEksCluster |[EksClusterScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/EksClusterScanner.java) | |
| AwsEmrCluster |[ElasticMapReduceClusterScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/ElasticMapReduceClusterScanner.java) | |
| AwsEmrClusterInstance |[ElasticMapReduceClusterScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/ElasticMapReduceClusterScanner.java) | |
| AwsElb | [ElbClassicScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/ElbClassicScanner.java)| |
| AwsElbTargetGroup | [ElbTargetGroupScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/ElbTargetGroupScanner.java) | |
| AwsHostedZone |[Route53Scanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/Route53Scanner.java)  | |
| AwsHostedZoneRecordSet |[Route53Scanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/Route53Scanner.java)  | |
| AwsInternetGateway | [AwsInternetGatewayScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/InternetGatewayScanner.java)  | |
| AwsLambdaFunction | [LambdaFunction](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/LambdaFunctionScanner.java)  | |
| AwsLaunchConfig | [LaunchConfigScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/LaunchConfigScanner.java) | |
| AwsLaunchTemplate |[LaunchTemplateScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/LaunchTemplateScanner.java)  | |
| AwsRdsCluster | [RdsCluster](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/RdsCluster.java) | |
| AwsRdsInstance | [RdsInstanceScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/RdsInstanceScanner.java) | |
| AwsRegion |[RegionScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/RegionScanner.java)  | |
| AwsRouteTable | [RouteTable](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/RouteTableScanner.java) | |
| AwsSecurityGroup |[SecurityGroupScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/SecurityGroupScanner.java)  | |
| AwsSnsTopic |[SnsScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/SnsScanner.java)  | |
| AwsSnsSubscription |[SnsScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/SnsScanner.java)  | |
| AwsSqsQueue |[SqsScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/SqsScanner.java) | |
| AwsElbListener | [ElbListenerScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/ElbListenerScanner.java) | |
| AwsS3Bucket | [S3Scanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/S3Scanner.java)| |
| AwsSubnet |[SubnetScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/SubnetScanner.java) | |
| AwsVpc |[VpcScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/VpcScanner.java) | |
| AwsVpcEndpoint |[VpcEndpointScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/VpcEndpointScanner.java) | |
| AwsVpcPeeringConnection |[VpcPeeringConnectionScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/VpcPeeringConnectionScanner.java) | |
| AwsVpnGateway |[VpnGatewayScanner](https://github.com/rebar-cloud/rebar-graph/blob/master/rebar-scanner-aws/src/main/java/rebar/graph/aws/VpnGatewayScanner.java) | |


## Naming Conventions

### Entity and Attributes

* All AWS entities should begin with `Aws` .  For example `AwsAccount`, `AwsRegion`, `AwsEc2Instance`, etc.
* All AWS nodes should have a `region` attribute. The values of region should be the lower case region names: `us-east-1`, `us-west-2`, etc.
* All attributes should use lower camel case.
* `arn` should use used wherever possible.  ARN's are globally unique.  Names are typically unique only by `account`-`region` pairs.  IDs tend to be `region`-unique, but have no guarantees of uniqueness across regions.
* Unique index for `arn` should be created if `arn` attribute is used.
* All AWS nodes should have `graphEntityType` set to the label of the node and `graphEntityGroup`=`aws`.  The `graphEntityType` property makes it easier for the code to know what kind of node it is using.
* Be very careful to qualify all non-unique attributes by `account` and `region`.  For instance, if you are looking up or modifying an `AwsElb`, with `{name:'foo'}`, be aware that this could match an ELB in any account or region.  It should be restricted with addition pattern matching attributes: `{name:'foo', account:'1111111111', region:'us-east-1}`
* The jackson Object-to-Json converter is used for most of the JSON serialization.  The AWS Java classes are auto-generated by Amazon in the SDK from the API specification.  This is a somewhat indirect representation of the underlying API, but in practice it works very well.



### Relationships

* Create only directed relationships.  Use *active* verb relationship names where possible.  *Passive* relationship names are OK when the active direction doesn't make a lot of sense. 
* When things contain other things, use the relationship name `HAS`
* When an entity makes reference to another entity, but doesn't "own" it: `USES` or `ATTACHED_TO`.  For instance, `(e:AwsEc2Instance)-[USES]->(s:AwsSecurityGroup)`
* **DO NOT create relationships to `AwsRegion` **.  It results in a _super-node_ with very little practical benefit.  
* **DO NOT create relationships to `AwsAccount`**  if there are other transitive ways to reach account.  For instance, anything that is related to a `AwsVpc` doesn't need its own relationship to `AwsAccount`, since it can be derived transitively and every object already has an `account` label.  However, `AwsS3Bucket` and `AwsRdsCluster` does not belong to a VPC, so it should have a relationship to account.   (Note: At some point in the future, I may consider changing this policy and mandating that everything has a relationship to `AwsAccount`.  However, I have no plans to do so yet.)

## Writing an AWS Scanner

* First, understand that the AWS APIs are _charmingly inconsistent_.  They are clearly all written by the same company and are mostly consistent with naming and such.  However there is a lot of subtle variation, including:

  * Some APIs use pagination.  Some don't.  The tokens that they use for pagination have different names.
  * Some refer to their entities by ARN.  Others by id.  Others by name.
  * Exceptions and behavior for entities that aren't found varies widely.
  * Performance varies widely across APIs.  Some hit rate limits easily.  Others don't.
  * etc.

* We are able to eliminate a lot of boilerplate from the scanners, but a fair amount remains.

* All AWS scanners should all extend `AwsEntityScanner<T>` where T is the type of the primary entity that they handle.

* `AwsEntityScanner` instances are *locked* to an `account-region` pair.

* The underlying `AwsScanner` takes care of sharing client connections.  In general you should simply call AwsEntityScanner.getClient( <builderClass>) to get an AWS client of your choice.  

* All scanners need to reference a `AwsEntityType` enum type.  For instance `AwsEc2Instance`, `AwsVpc`, etc.

  In general, doScan() should keep going when exceptions are encountered.  It is encouraged to use `tryExecute()` to handle exceptions correctly as it loops through returned items.

* When single entities are scanned, exceptions should *NOT* be caught.  The thinking here, is that if you are scanning a specific item and it fails, you probably want to know.  However, if you are scanning a whole `account-region`, you want as much of the data as possible.

* It is recommended to use the GraphDB DSL for most graph mutations rather than writing cypher directly.  It eliminates **a lot** of error-prone boilerplate.

  

## Dealing With Deletion

Dealing with deleted items is one of the tricker parts of maintaining the graph.  What has worked best is a 2-phase operation.  When a full scan of a given type in an `account-region` tuple is complete, there is code that loads all the entities for that type for that given `account-region` pair.  We then look at the timestamp of each node that was loaded.  If `graphUpdateTs` is less than the start time of the operation, we know that the entity _may_ have been deleted.

To find out if the entity has been deleted or not, we need to make a single _get_ or _describe_ operation for that entity.  If AWS responds definitively that the item does not exist, we are free to delete it from the graph.

There is some complexity with the multi-tenant data model. We need to make sure that we are only operating on items from the `account-region` tuple that our scanner is configured to use.  Calling _describe_ or _get_ on an entity from another account or region would cause AWS to return with some variant of _NotFound_, which we might cause us to delete an entity in our `account-region`.