# rebar-graph-driver-neo4j

Fluent driver for working with Neo4j graph database as JSON objects.



## Usage

### Instantiating 

The driver is thread-safe and should be shared.

To create a `Neo4jDriver` instance around local database without encryption:

```java
Neo4jDriver driver = new Neo4jDriver.Builder().build();
```

With url, username and password:

```java
driver = new Neo4jDriver.Builder()
    .withUrl("bolt://neo4j.example.com:7687")
    .withCredentials("myusername", "mypassword")
    .build();
```

If you already have a Neo4j Bolt `Driver` instance or want to build it yourself:

```java
Driver bolt = 
    GraphDatabase.driver(
        "bolt://neo4j.example.com", 
        AuthTokens.basic("myusername", "mypassword"));
		
Neo4jDriver driver = new Neo4jDriver.Builder()
                        .withDriver(bolt)
                        .build();
```

### Query

Responses are returned as `Stream<JsonNode>` objects.  This makes the results easy to process.

Find all people born in 1971:
```java
driver.cypher("match (a:Person {born:1971}) return a order by a.name")
				.stream()
				.forEach(System.out::println);
```

Output:
```json
{"born":1971,"name":"Corey Feldman"}
{"born":1971,"name":"Noah Wyle"}
{"born":1971,"name":"Paul Bettany"}
{"born":1971,"name":"Regina King"}
{"born":1971,"name":"Rick Yune"}
```

If we want just a list of names:

```java
driver.cypher("match (a:Person {born:1971}) return a order by a.name")
	.stream()
	.map(n->n.path("name").asText())
	.forEach(System.out::println);
```

Output:
```
Corey Feldman
Noah Wyle
Paul Bettany
Regina King
Rick Yune
```

Cypher statements can be parameterized:

```java
driver.cypher("match (a:Person {born:{birthYear}}) return a order by a.name")
	.param("birthYear",1975)
	.stream()
	.forEach(System.out::println);
```

Output:
```
{"name":"Charlize Theron","born":1975}
```

Parameters can also be passed as part of a JsonNode.

```java
JsonNode params = 
    new ObjectMapper()
    .createObjectNode()
    .put("birthYear", 1968);

driver.cypher("match (a:Person {born:{birthYear}}) return a order by a.name")
	.param(params)
	.stream()
	.forEach(System.out::println);
```

Output:

```java
{"born":1968,"name":"Cuba Gooding Jr."}
{"born":1968,"name":"Dina Meyer"}
{"born":1968,"name":"Orlando Jones"}
{"born":1968,"name":"Parker Posey"}
{"born":1968,"name":"Sam Rockwell"}
```

Modifying data is similar:

```java
driver.cypher("merge (a:Person {name:{name}}) set a.born={birthYear} return a")
	.param("name","Rob Schoening")
	.param("birthYear",1975)
	.stream()
	.forEach(System.out::println);
```

Output:
```
{"born":1975,"name":"Rob Schoening"}
```

Json structures can be passed in conveniently.  Note that `__params` will contain all the parameters.
This makes is trivial to set many properties at once.

```java
JsonNode params = 
    new ObjectMapper()
    .createObjectNode()
    .put("name","Rob Schoening")
    .put("birthYear", 1975)
    .put("occupation","Engineer");
    
driver.cypher("merge (a:Person {name:{name}}) set a={__params} return a")
	.param(params)
	.stream()
	.forEach(System.out::println);
```

Output:
```
{"occupation":"Engineer","birthYear":1975,"name":"Rob Schoening"}
```
