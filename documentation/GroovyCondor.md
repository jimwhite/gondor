---
layout: page
title: Gondor = Groovy Condor
---

Gondor is Groovy Condor, is a fluent scripting language for writing Condor DAGman workflows. Gondor is a work in progress with several innovative features in the pipeline, including job memoization using Git, workflow reduction, and support for provenance-aware results.

##The Components: HT Condor and Groovy
[HTCondor](http://research.cs.wisc.edu/htcondor/description.html) is a full-featured batch processing system for compute-intensive workloads and is widely used in scientific research.  It has also been a significant part of the Big Data story in industry having been the foundation for a series of record-breaking [cloud (AWS EC2) computing runs](http://www.cyclecomputing.com/discovery-invention/use-cases/).

##Gondor Features
As a result of combining Groovy with HTCondor, Gondor has the following features: 
* Self-describing Command line
* Dynamic SubDAG workflow scripts
* Workflow reduction
* Provenance
* Reproducible research
