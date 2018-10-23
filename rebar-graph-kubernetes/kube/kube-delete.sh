#!/bin/bash



kubectl delete deployment rebar-graph-kubernetes
kubectl delete ClusterRoleBinding rebar-graph
kubectl delete ClusterRole rebar-graph
kubectl delete ServiceAccount rebar-graph