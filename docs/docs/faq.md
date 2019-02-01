---
id: faq
title: FAQ
sidebar_label: FAQ
---

## Is Rebar a framework?

**No.** Rebar is unopinionated about how you design, build and operate your systems.  It is designed to be added to your infrastructure without need
for structural modification.  

We don't care if you use scripts, Terraform, CloudFormation, Helm, Spinnaker.  Or god forbid, you manage your AWS with the Console, that's fine.  (Actually it's not fine, but rebar doesn't care.)

## Is it a Platform-as-a-Service?

No.  At my previous employer we did build a PaaS on top of this architecture and this worked very well.  However, Rebar is not a PaaS.

Use your own PaaS, CloudFoundry, whatever.  If you *are* using a PaaS like CloudFoundry, I'm pretty sure that a significant portion of your critical infrastructure *isn't* deployed through your PaaS though.  

That's why you shoud care about Rebar.

## Is it an asset management / discovery tool?

Yes.  But asset management tools suck.  Asset management sounds like something from the wrong decade.  When developers hear the words __asset management__ they flee.  

But Rebar is fun and cool. It does the things that governance, compliance, security, and accounting care about when they say "asset management", but it is implemented in a cloud-native way with great technology, not the crufty old system that you are using to manage your datacenter fixed asssets.

## Why do I need this graph data model when there is an API for everything?

This is a good question.  At small scale, tying things together with API calls works fine.  But as soon as your infrastructure has enough dimensions (enivronments, teams, providers, services, etc.) even seemingly simple tasks can start to get quite complicated.   

Take this example:

> I want to deploy a new version of `XYZ` to https://cool.example.com, but what account/region is it in?  Which ELB handles it?

Yes, these answers can be had with API calls.  But in practice, it is surprisingly difficult.  Not impossible, just hard enough to dissuade.  You start running into N^2 problems and/or running up against mundane problems like managing auth credentials for all your tools/scripts.   Things become just difficult enough that neglect sets in.

On the other hand, with a graph database with all your data pre-aggregated and normalized, you can get an accurate answer with a simple query against a graph database.

No code!  

## I'm using AWS/GCP/Azure, why would I need this?

Your infrastructure is likely spread across many accounts and segmented into regions.  Cloud providers do this to maximize availability and isolate regions into idependent failure domains.  However, that comes at a cost of significant management complexity.

> You think you have infrastructure running in `us-east-1` and `us-west-2`.  But do you have anything in `eu-west-2`?  Are you sure? 
> Does `XYZ` run in GCP or AWS or both?

These are the kind of simple questions that can be quite time consuming to answer.  With Rebar, they are simple.

To cite a simple example, in our experience, cloud providers tend to send notifications about infrastructure without much context.  The following is common:

> _"Thanks for telling me that `i-ab38d8273ff4` is about to die.  But what account is it in?  What region?  What is it for?  Who is responsible for it?
> What will happen when it is terminated?"_

You're on your own.  Easy to automate?  Yes.  But will you?  You probably won't.  You have other things to do.  I'm going to go out on a limb and say that eventually someone will be surprised when `i-ab38d8273ff4` is terminated.  You knew that `i-ab38d8273ff4` was going to die.  You received that email day after day.

But figuring out what account `i-ab38d8273ff4` is in and what it is used for would have been too much.  You have other more important things to do.

I've been there, obviously.

## I'm using Terraform/CloudFormation/etc, why would I need this?

I'm going to take wild guess and say that only some subset of your infrastructure is actually managed by Terraform or CloudFormation or your management tool of choice.  But which services are using them correctly?  Which aren't?  These are the questions we can answer quickly and easily with Rebar.

Besides, how many people in your org actually understand that Terraform / CloudFormation you wrote?  I'll bet that it is a lot fewer than you think.

I love Terraform, but it reminds me of a joke from the parody _DevOps Borat_ twitter account:

    "[Terraform] is like teenage sex... everyone's talking about it... everyone SAYS they are doing it... but how many people are really doing it?"

## I'm using Kubernetes, why would I need this?

First, if you are running anything more than a PoC, you probably have a few Kubernetes clusters.  You might have some OpenShift on-prem and some EKS clusters spread across accounts and regions.  If you want find all the services that are using an image with a critical CVE, how are you going to do that without going on a spelunking exercise?

Again, it is not that this is an insurmountably difficult problem to solve.  In our experience, you just won't prioritize it.

You. Just. Won't.

Again, for good reason.  You have important things to do.

Rebar fixes this by lifting this data up and making it accessible to anybody that can write a Neo4j Cypher query.

## But I have Chef/Puppet/Ansible/Salt, why would I need this?

That's funny. I can't wait for these tools to die.

They are elaborate life-support system from an operational model from the last decade.

If they work for you, that's great.  But my experience with all of them is that they need a large amount of maintenance.  We used rebar
just to monitor which puppet agents and salt minions were alive and which were dead.

## How do I run Neo4j on my laptop?

I use this:

```bash
docker run \
    -d --rm \
    --publish=7474:7474 --publish=7687:7687 \
    --env=NEO4J_ACCEPT_LICENSE_AGREEMENT=yes \
    --env=NEO4J_AUTH=none \
    --name=neo4j \
    neo4j:3.5
```

Once the container starts, you can access it from your browser at:

[http://localhost:7474](http://localhost:7474)

If you laumch a rebar scanner container on the same host, the Neo4j database url will be:

`bolt://host.docker.internal:7687`



