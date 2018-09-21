package zemberek.grpc.testclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import zemberek.core.logging.Log;
import zemberek.grpc.server.ZemberekGrpcServer;
import zemberek.proto.AnalysisRequest;
import zemberek.proto.AnalysisResponse;
import zemberek.proto.AnalysisServiceGrpc;
import zemberek.proto.AnalysisServiceGrpc.AnalysisServiceBlockingStub;
import zemberek.proto.DetectRequest;
import zemberek.proto.DetectResponse;
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

public class TestClient {

  public static void main(String[] args) {
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", ZemberekGrpcServer.DEFAULT_PORT)
        .usePlaintext()
        .build();
    AnalysisServiceBlockingStub analysisServiceBlockingStub = AnalysisServiceGrpc
        .newBlockingStub(channel);
    LanguageIdServiceBlockingStub languageIdServiceBlockingStub = LanguageIdServiceGrpc
        .newBlockingStub(channel);
    PreprocessingServiceBlockingStub preprocessingServiceBlockingStub =
        PreprocessingServiceGrpc.newBlockingStub(channel);
    NormalizationServiceBlockingStub normalizationServiceBlockingStub =
        NormalizationServiceGrpc.newBlockingStub(channel);

    Log.info("----- Morphological Analysis ------------ ");
    String input = "tapirler";
    AnalysisResponse response = analysisServiceBlockingStub.analyze(AnalysisRequest.newBuilder()
        .setInput(input)
        .build());
    Log.info("Input: " + input);
    Log.info("Response: " + response);

    Log.info("----- Language Identification ------------ ");
    String langIdInput = "Merhaba dünya";
    DetectResponse langIdResponse = languageIdServiceBlockingStub.detect(
        DetectRequest.newBuilder().setInput(langIdInput).build());
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
