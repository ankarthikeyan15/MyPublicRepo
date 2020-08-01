# MyPublicRepo
This repo contains my test projects

The CPUAllocator program implements an instance allocation application for a Cloud Operator that allocates the resources according to the user requirements

## INPUT Setup

1. Add instances data in resource/instances.json in the following format:

#Example
```
{
"us-east": {
"large": 0.13,
"xlarge": 0.23,
"2xlarge": 0.45,
"4xlarge": 0.774,
"8xlarge": 1.4,
"10xlarge": 2.82
},
"us-west": {
"large": 0.12,
"2xlarge": 0.413,
"4xlarge": 0.89,
"8xlarge": 1.3,
"10xlarge": 2.97
}
}
```

2. Add the input data to the cloudResource.properties file as:
```
#To request servers with minimum total cost for the given minimum number of CPUs and hours:
#hours=
#minCPUs=

#To request servers with as many possible CPUs for a given maximum Price:
#hours=
#maxPrice=

#To request servers for a given minimum number of CPUs and a given maximum Price:
#hours=
#minCPUs=
#maxPrice=

#Example
hours=24
minCPUs=115
maxPrice=207.85
```

## RUN CPUAllocator
```
java CPUAllocator
```
