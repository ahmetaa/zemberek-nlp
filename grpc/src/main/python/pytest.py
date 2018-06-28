#!/usr/bin/env python
# -*- coding: utf-8 -*-

import grpc

import language_id_pb2
import language_id_pb2_grpc

def find_lang_id(stub, i):
    response = stub.detectLanguage(language_id_pb2.LangIdRequest(input=i))
    result = response.langId
    print(result)


def run():
    channel = grpc.insecure_channel('localhost:6789')
    stub = language_id_pb2_grpc.LanguageIdServiceStub(channel)
    find_lang_id(stub, 'merhaba dünya')

if __name__ == '__main__':
    run()