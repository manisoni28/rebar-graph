---
id: faq
title: FAQ
sidebar_label: FAQ
---

## Is Rebar a framework?

**No.** Rebar is unopinionated about how you design, build and operate your systems.  It is designed to be added to your infrastructure without need
for structural modification.

## Is it a Platform-as-a-Service?

No.  At my previous employer we did build a PaaS on top of this architecture and this worked very well.  However, Rebar is not a PaaS.

Use your own PaaS, CloudFoundry, whatever.  We are pretty sure that not *ALL* of your infrastructure will be in the PaaS though.

## Is it an asset management / discovery tool?

Asset management tools suck.  When developers hear the words __asset management__ they flee.  Rebar is fun and cool.  Rebar does do many of the things that asset management tools do but it is shiny new and modern and your dev team will love working with it.

## Why do I need this graph data model when there is an API for everything?

It is a good question.  At small scale, tying things together with API calls works fine.  But as soon as your infrastructure has enough dimensions (enivronments, teams, providers, services, etc.) even seemingly simple tasks can start to get quite complicated.   

> I want to deploy a new version of `XYZ` to https://cool.example.com, but what account/region is it in?  Which ELB handles it?

Yes, these answers can be had with API calls.  But in practice, it is surprisingly difficult.  You start running into N^2 problems and/or running up against mundane problems like managing auth credentials for all your tools/scripts.   Things become just difficult enough that neglect sets in.

On the other hand, if you can get a real-time answer with a simple query against a graph database, problems that might have been daunting can seem almost effortless (and fun).

## I'm using AWS/GCP/Azure, why would I need this?

Your infrastructure is likely spread across many accounts and segmented into regions.  Cloud providers do this to maximize availability and isolate regions into idependent failure domains.  However, that comes at a cost of significant management complexity.

> You think you have infrastructure running in `us-east-1` and `us-west-2`.  But do you have anything in `eu-west-2`?  Are you sure? 
> Does `XYZ` run in GCP or AWS or both?

These are the kind of simple questions that can be quite time consuming to answer.  With Rebar, they are simple.

To cite a simple example, in our experience, cloud providers tend to send notifications about infrastructure without much context.  The following is common:

> _"Thanks for telling me that `i-ab38d8273ff4` is about to die.  But what account is it in?  What region?  What is it for?  Who is responsible for it?
> What will happen when it is terminated?"_

You're on your own.  Easy to automate?  Yes.  But will you?  Probably not. 

## I'm using Terraform/CloudFormation/etc, why would I need this?

I'm going to take wild guess and say that only some subset of your infrastructure is actually managed by Terraform or CloudFormation or whatever your management tool of choice.  But which services are using them correctly?  Which aren't?  These are the questions we can answer quickly and easily with Rebar.

Besides, how many people in your org actually understand that Terraform / CloudFormation you wrote?  I'll be that it is a lot fewer than you think.

## I'm using Kubernetes, why would I need this?

First, if you are running anything more than a PoC, you probably have a few Kubernetes clusters.  You might have some OpenShift on-prem and some EKS clusters spread across accounts and regions.  If you want find all the services that are using an image with a critical CVE, how are you going to do that without going on a spelunking exercise?

Again, it is not that it is a hard problem to solve.  In our experience, you just won't prioritize it, so you die a slow death of 1000 cuts.

Rebar fixes this.

## But I have Chef/Puppet/Ansible/Salt, why would I need this?

That's funny. In our experience, we needed Rebar just to figure out whether those tools were deployed and working correctly.
