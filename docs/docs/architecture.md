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