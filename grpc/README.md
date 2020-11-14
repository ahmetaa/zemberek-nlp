Zemberek gRPC Server
============

## Introduction

Zemberek-NLP provides some of its functions via a remote procedure call framework called `gRPC`.
[gRPC](https://grpc.io) is a high performance, open-source universal RPC framework. Once Zemberek-NLP 
gRPC server is started, other applications can access remote services natively via automatically 
generated client libraries. gRPC supports many languages out of the box such as Java, Python, C++, C#, Node.js etc.

Initially only a limited number of functions are available and only Python client library
is provided. 

All remote API is subject to change without notice until version 1.0.0.
 
## Running the gRPC server.

For starting the gRPC server, use zemberek-full.jar and run:

    java -jar zemberek-full.jar StartGrpcServer

By default, it will start serving via port 6789. User can change the port with `--port` parameter.

For some functions like normalization, model files may be necessary. For this, user should provide
data root directory with `--dataRoot` parameter. For example, if data folder is 
`/home/aaa/zemberek-data` user should run the server with:

    java -jar zemberek-full.jar StartGrpcServer --dataRoot /home/aaa/zemberek-data

## Remote API

gRPC remote services are defined in [protocol buffers](https://developers.google.com/protocol-buffers/) 3 
Interface Definition Language (IDL). For example, for language identification (simplified):

    message LanguageIdRequest {
      string input = 1;
    }
    
    message LanguageIdResponse {
      string langId = 1;
    }
    
    service LanguageIdService {
      rpc Detect (LanguageIdRequest) returns (LanguageIdResponse);
    }

For accessing remote API from Python, you need to install grpc related libraries.

    pip install grpcio-tools
    pip install googleapis-common-protos  

[here](https://github.com/ahmetaa/zemberek-nlp/tree/master/grpc/src/main/python) python sources
for accessing grpc server can be found. An example usage:
 

    #!/usr/bin/env python3
    # -*- coding: utf-8 -*-
    
    import grpc

    import language_id_pb2 as z_langid
    import language_id_pb2_grpc as z_langid_g
    
    channel = grpc.insecure_channel('localhost:6789')
    
    langid_stub = z_langid_g.LanguageIdServiceStub(channel)
    
    def find_lang_id(i):
        response = langid_stub.Detect(z_langid.LanguageIdRequest(input=i))
        return response.langId
    
    def run():
        lang_detect_input = 'merhaba dünya'
        lang_id = find_lang_id(lang_detect_input)
        print("Language of %s is: %s" % (lang_detect_input, lang_id))
        
    if __name__ == '__main__':
        run() 

For a full example check zemberek_client_text.py file 
