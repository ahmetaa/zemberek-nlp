#!/usr/bin/env python
# -*- coding: utf-8 -*-

import grpc

import language_id_pb2
import language_id_pb2_grpc
import normalization_pb2
import normalization_pb2_grpc
import preprocess_pb2
import preprocess_pb2_grpc
import simple_analysis_pb2
import simple_analysis_pb2_grpc

channel = grpc.insecure_channel('localhost:6789')

langid_stub = language_id_pb2_grpc.LanguageIdServiceStub(channel)
normalization_stub = normalization_pb2_grpc.NormalizationServiceStub(channel)
preprocess_stub = preprocess_pb2_grpc.PreprocessingServiceStub(channel)
simple_analysis_stub = simple_analysis_pb2_grpc.SimpleAnalysisServiceStub(channel)

def find_lang_id(i):
    response = langid_stub.Detect(language_id_pb2.DetectRequest(input=i))
    return response.langId

def tokenize(i):
    response = preprocess_stub.Tokenize(preprocess_pb2.TokenizationRequest(input=i))
    return response.tokens

def normalize(i):
    response = normalization_stub.Normalize(normalization_pb2.NormalizationRequest(input=i))
    return response

def analyze(i):
    response = simple_analysis_stub.AnalyzeSentence(simple_analysis_pb2.SentenceAnalysisRequest(input=i))
    return response;

def run():
    lang_detect_input = 'merhaba dünya'
    lang_id = find_lang_id(lang_detect_input)
    print("Language of : " + lang_detect_input.decode("utf-8"))
    print(lang_id)

    print("")
    tokenization_input = 'Merhaba dünya!'
    print('Tokens for input : ' + tokenization_input.decode("utf-8"))
    tokens = tokenize(tokenization_input)
    for t in tokens:
        print(t.token + ':' + t.type)

    print("")
    normalization_input = 'Mrhaba dnya'
    print('Normalization result for input : ' + normalization_input.decode("utf-8"))
    n_response = normalize(normalization_input)
    if n_response.normalized_input:
        print(n_response.normalized_input)
    else:
        print('Problem normalizing input : ' + n_response.error)


    print("")
    analysis_input = 'Kavanozun kapağını açamadım.'
    print('Analysis result for input : ' + analysis_input.decode("utf-8"))
    analysisResult = analyze(analysis_input)
    for a in analysisResult.results:
        best = a.best
        lemmas = ""
        for l in best.lemmas:
          lemmas = lemmas + " " + l
        print("Word = " + a.token + ", Lemmas = " + lemmas + ", POS = [" + best.pos + "], Full Analysis = {" + best.analysis + "}")


if __name__ == '__main__':
    run()