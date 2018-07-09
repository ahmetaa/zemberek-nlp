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

public class TestClient {

  public static void main(String[] args) {
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", ZemberekGrpcServer.PORT)
        .usePlaintext()
        .build();
    AnalysisServiceBlockingStub analysisServiceBlockingStub = AnalysisServiceGrpc
        .newBlockingStub(channel);
    LanguageIdServiceBlockingStub languageIdServiceBlockingStub = LanguageIdServiceGrpc
        .newBlockingStub(channel);
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

  }
}
