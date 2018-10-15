Zemberek gRPC Server (Not Yet Released)
============

## Introduction

Zemberek-NLP provides some of its functions via a remote procedure call framework called `gRPC`.
[gRPC](https://grpc.io) is a high performance, open-source universal RPC framework. Once Zemberek-NLP 
gRPC server is started, other applications can access remote services natively via automatically 
generated client libraries. gRPC supports many languages out of the box (Java, Python, C++, C#, Node.js etc.).

Initially only a limited number of functions are available and only Java and Python client libraries
are provided. 

All remote API is subject to change without notice until version 1.0.0.
 
## Running the gRPC server.

For starting the gRPC server, use zemberek-full.jar and run:

    java -jar zemberek-full.jar StartGrpcServer

By default, it will start serving via port 6789. User can change the port with --port parameter.

## Remote API

gRPC remote services are defined in [protocol buffers](https://developers.google.com/protocol-buffers/) 3 
Interface Definition Language (IDL).  



