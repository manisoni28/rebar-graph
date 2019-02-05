# rebar-scanner-google-cloud

Constructs a graph model of Google Cloud Entities



## Sample Queries

```
match (p:GcpProject)--(x)--(z:GcpZone)--(r:GcpRegion) return p,x,z,r;
```


