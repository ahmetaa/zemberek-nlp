package zemberek.grpc.testclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import zemberek.core.logging.Log;
import zemberek.grpc.server.ZemberekGrpcServer;
import zemberek.proto.LanguageIdRequest;
import zemberek.proto.LanguageIdResponse;
import zemberek.proto.LanguageIdServiceGrpc;
import zemberek.proto.LanguageIdServiceGrpc.LanguageIdServiceBlockingStub;
import zemberek.proto.NormalizationRequest;
import zemberek.proto.NormalizationResponse;
import zemberek.proto.NormalizationServiceGrpc;
import zemberek.proto.NormalizationServiceGrpc.NormalizationServiceBlockingStub;
import zemberek.proto.PreprocessingServiceGrpc;
import zemberek.proto.PreprocessingServiceGrpc.PreprocessingServiceBlockingStub;
import zemberek.proto.SentenceExtractionRequest;
import zemberek.proto.SentenceExtractionResponse;
import zemberek.proto.TokenizationRequest;
import zemberek.proto.TokenizationResponse;
import zemberek.proto.morphology.MorphologyServiceGrpc;
import zemberek.proto.morphology.MorphologyServiceGrpc.MorphologyServiceBlockingStub;
import zemberek.proto.morphology.SentenceAnalysisProto;
import zemberek.proto.morphology.SentenceAnalysisRequest;
import zemberek.proto.morphology.WordAnalysisProto;
import zemberek.proto.morphology.WordAnalysisRequest;

public class TestClient {

  public static void main(String[] args) {
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", ZemberekGrpcServer.DEFAULT_PORT)
        .usePlaintext()
        .build();
    MorphologyServiceBlockingStub analysisService = MorphologyServiceGrpc
        .newBlockingStub(channel);
    LanguageIdServiceBlockingStub languageIdServiceBlockingStub = LanguageIdServiceGrpc
        .newBlockingStub(channel);
    PreprocessingServiceBlockingStub preprocessingServiceBlockingStub =
        PreprocessingServiceGrpc.newBlockingStub(channel);
    NormalizationServiceBlockingStub normalizationServiceBlockingStub =
        NormalizationServiceGrpc.newBlockingStub(channel);

    Log.info("----- Word Morphological Analysis ------------ ");
    String input = "tapirler";
    WordAnalysisProto response = analysisService.analyzeWord(WordAnalysisRequest.newBuilder()
        .setInput(input)
        .build());
    Log.info("Input: " + input);
    Log.info("Response: " + response);

    Log.info("----- Sentence Morphological Analysis ------------ ");
    String sentence = "Ali Kaan okula gitti mi?";
    SentenceAnalysisProto sResponse = analysisService.analyzeSentence(
        SentenceAnalysisRequest.newBuilder()
            .setInput(sentence)
            .build());
    Log.info("Input: " + sentence);
    Log.info("Response: " + sResponse);

    Log.info("----- Language Identification ------------ ");
    String langIdInput = "Merhaba dünya";
    LanguageIdResponse langIdResponse = languageIdServiceBlockingStub.detect(
        LanguageIdRequest.newBuilder().setInput(langIdInput).build());
    Log.info("Input: " + langIdInput);
    Log.info("Response: " + langIdResponse.getLangId());

    Log.info("----- Tokenization ------------ ");
    String tokenizationInput = "Saat, 12:00.";
    TokenizationResponse tokenizationResponse = preprocessingServiceBlockingStub
        .tokenize(TokenizationRequest.newBuilder()
            .setInput(tokenizationInput)
            .setIncludeTokenBoundaries(true)
            .build());
    Log.info("Input: " + tokenizationInput);
    Log.info(tokenizationResponse);

    Log.info("----- Sentence Extraction ------------ ");
    String sentenceExtractionInput = "Merhaba! Bugün 2. köprü Fsm.'de trafik vardı.değil mi?";
    SentenceExtractionResponse sentenceExtractionResponse = preprocessingServiceBlockingStub
        .extractSentences(SentenceExtractionRequest.newBuilder()
            .setDocument(sentenceExtractionInput)
            .build());
    Log.info("Input: " + sentenceExtractionInput);
    sentenceExtractionResponse.getSentencesList().forEach(Log::info);

    Log.info("----- Normalization ------------ ");
    String normalizationiInput = "Merhab ben Zemberk.";

    NormalizationResponse normalizationResponse = normalizationServiceBlockingStub
        .normalize(NormalizationRequest.newBuilder()
            .setInput(normalizationiInput)
            .build());
    Log.info("Input: " + normalizationiInput);
    Log.info("Response: " + normalizationResponse.getNormalizedInput());
  }
}
