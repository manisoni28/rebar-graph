---
id: architecture
title: Architecture
sidebar_label: Architecture
---
# Problem Domain

Rebar's problem domain is the intersection between:

* Development
* Operations
* Security 
* Finance 
* Risk / Compliance

I know from experience that the intersection of these domains is where all the pain is!  The DevOps story is well known and a generally accepted 
solution. But there is just as much _sadness_ and _misery_ in these other intersecting domains.

* What developer says she is excited about that upcoming SOX audit?
* What operations engineer is excited about the patching wave for the latest 0-day vulnerability?
* What DevOps engineer is excited to explain the nuances of AWS billing to the CFO who just wants predictable budgets and likes CapEx purchasing patterns?

None. None. And none!

Rebar is all about removing sadness.  It's about taking these miserable things and helping you solve them and make things as fun as they looked at re:Invent, OSCON, Velocity or QCon!

# Database

## Neo4j

Rebar uses Neo4j as its primary datastore.

I've tried to solve this infrascture management problem over the years with just about every type of database.  It wasn't until I started using graph databases that things started falling in place.

Why?

**Neo4j sparks joy!**

Seriously.  I've enjoyed using the Neo4j graph database more than just about any other piece of technology in my career.  It is JSON-native and has a delightfully intuitive query language.

It works extremely well for managing infrastructure because:

__No Schema__

I do a lot of work in FinTech and I usually do NOT see schema-less databases as having a lot of virtue.  Constraints have value.  I suppose that I like schema constraints for the same reason that I like strongly typed compiled languages like Java, Go, etc.  Constraints, like lane markers and guardrails keep you safe and make it *easier*, not harder to go fast, safely.

But for infrastructure management, being able to model entitites, attributes and relationships fluidly is much more important.  Things change quickly.  If you find that you are missing some key metadata on your entities, with Neo4j you can _just add them_.  There is no reason to wait.

For instance if you find that you need to keep track of patching status while you are addressing a 0-day vulnerability, just add it.

__Real-World Relationships Change Constantly__

We try to capture all the implicit relationships in the systems that Rebar scans.

However, you probably have a lot more relationships that aren't a part of Rebar.  You can just load this data and establish those relationships as you see fit.  Again, there is no need to go through a software development cycle.  Again, just do it.

__Infrastructure Is A Graph__

With a graph database, it is trivial to *join* entities across domains.  Code resides in Github repositories, is built using CI jobs, baked into images, deployed in to clusters, which run on instances.  Each of these things typically has one or ore people responsible for it.  

Obvious, right?  So why aren't you using a database that can model all these things for you?

__Neo4j is Just Cool__

I challenge you to try it and not smile.

# Deployment Topology

## Scanners

Each of the rebar dicscovery tools, called _scanners_ is provided as a Docker image and deployed as a Docker container.  

You are free to schedule these containers however you like, with Kubernetes or just plain docker. You might even find that it's enough just to run it on your laptop!

In general the rebar scanners only need to connect to:

* Data System - Cloud control planes (AWS), Kubernetes, GitHub, etc.
* Neo4j - Where the data will be stored

The deployment model is one-container-per-source.  If you have 5 AWS regions, one GitHub server, and 5 Kubernetes clusters, then you would run 11 rebar scanners, one for each.  

It is worth noting that a single AWS scanner is able to connect to multiple regions, but each account will need its own rebar-scanner instance to ingest data.

## Database

You will need a Neo4j database.  The OSS version is free and very easy to manage.  Easy like Redis is easy.  

If you want clustered HA, you can purchase that from Neo4j.

_Note: We suggest that you use the graph much as you would use a cache.  That is, don't use it as a system of record for anything important.  We blew up Neo4j once a few years ago.  Fixing the problem was trivial.  We just nuked the database and a few minutes later, all the data had been reconstitued!_

## Dashboard

A simple Dashboard will be available soon.  You might want to create your own.  

## API Server

An API server will be available soon.  This will allow queries to be easily scripted with curl and grep.

I'm interested in creating a small golang-based binary client.  But `curl | grep` or `curl | awk` is probably all you need. 


# Design Philosophy

Rebar reinforced concrete enhances concrete's compressive and tesnsile strength by up to 100x, without dramatically increasing the cost of construction. A modest amount of expensive steel combined with inexpensive concrete can be used to create structures that are cost-effective and resilient.

That's what we're trying to do with rebar.  Rebar is not intended to be a framework or infrastructure foundation.  The goal is to be a structural additive that can make your technology organization more resilient.

The goal of rebar is to make it easier for you to do whatever you are trying to build.  Unless you are Amazon, running dozens of AWS accounts doesn't generate money for you.  It's a lot of hassle and risk to do it well.  10x harder than whatever you saw at the last re:Invent session.  And if you reading this and disagreeing, I'm going to go out on a limb and say that you're probably just deferring all your ownership maintenance costs into a pile of technical debt that will crush you later.  

Similarly running dozens of kubernetes clusters isn't really a fun thing.  Making money and profit from them is a great thing.  And if those kubernetes clusters are helping you to achieve that, that's great.  But that doesn't change the fact that this infrastructure is still a pain in the ass to manage and nobody really cares.  Rebar doesn't try to solve any of this directly.  What it does do is make it _easier for you to solve those problems_. 

# Testing Methodology

Testing needs to be pragmatic.  Here are some thoughts.

1. Tests need to be fun to write and satisfying to execute.  Like a clean house, you cannot reduce the satisfaction of cleaning to some bullshit metric like clutter per square foot.  You just say, "This feels good."  The same is true of testing.
2. Tests need to be fast.  So fast that the cost of running them relative to everything else you do is inconsequential.  A few minutes max.
3. We should try to test what we can using unit tests with  no external dependencies, because those tests are blazingly fast and usually very simple.  But let's not bend the architecture in unnatural ways in order to achieve this.  Unit tests are great, but they often don't provide as much benefit as you might think.  
4. Use mocking sparingly.  Your mocked tests are definitely not as useful as you think they are.
5. There is no substitute for tests that exercise the whole system as it is designed to run in production.  Duh.
6. Allow the test system to support selective degradation.  If I don't have a database instance for the tests, don't fail those tests.  Just skip them.  If I don't have connectivity into a cloud environment, don't fail those tests.  Just skip them.
7. Allow test-skipping to prohibited.  For instance, in a controlled CI environment where we have good control of dependencies, it may be fine to say "Database tests cannot be skipped."  "AWS Tests cannot be skipped".  But for the love of god, don't enforce those requirements everywhere and always.
8. We will NOT spend a lot of time manipulating cloud environments to known configuration in order to test the software.  I've tried this and it is giant waste of time.  What you'll invariably find with AWS and Kubernetes is that those systems are slow and unreliable relative to what you are testing.  You'll spend more time writing code to deal with timeouts and asynchronous conditions than writing meaningful tests.  "Did the test fail or did Kubernetes just take 5x longer than normal?"  That.
9. For a lot of rebar, we actually don't care too much *what* data is in the system, just that there *is* data in the system.  That is we can run it against a target system and test that all the data that we have is conformant.  Do the attributes look right?  Do we have the kinds of relationships that we expect? etc.