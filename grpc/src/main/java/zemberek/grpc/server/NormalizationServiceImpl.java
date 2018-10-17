package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.proto.NormalizationInitializationRequest;
import zemberek.proto.NormalizationInitializationResponse;
import zemberek.proto.NormalizationRequest;
import zemberek.proto.NormalizationResponse;
import zemberek.proto.NormalizationServiceGrpc.NormalizationServiceImplBase;

public class NormalizationServiceImpl extends NormalizationServiceImplBase {

  ZemberekContext context;

  private Path dataRoot;
  private Path lmPath;
  TurkishSentenceNormalizer sentenceNormalizer;

  public NormalizationServiceImpl(ZemberekContext context) {
    this.context = context;
  }

  @Override
  public synchronized void initialize(NormalizationInitializationRequest request,
      StreamObserver<NormalizationInitializationResponse> responseObserver) {
    dataRoot = Paths.get(request.getNormalizationDataRoot());
    lmPath = Paths.get(request.getLanguageModelPath());
    String error = "";
    try {
      sentenceNormalizer = new TurkishSentenceNormalizer(
          context.morphology, dataRoot, lmPath
      );
    } catch (Exception e) {
      e.printStackTrace();
      error = e.getMessage();
    }
    responseObserver
        .onNext(NormalizationInitializationResponse.newBuilder().setError(error).build());
    responseObserver.onCompleted();
  }

  @Override
  public void normalize(NormalizationRequest request,
      StreamObserver<NormalizationResponse> responseObserver) {

    String normalized;
    if (sentenceNormalizer != null) {
      String s = request.getInput();
      normalized = sentenceNormalizer.normalize(s);
      responseObserver.onNext(NormalizationResponse.newBuilder()
          .setNormalizedInput(normalized)
          .build());
    } else {
      responseObserver.onNext(NormalizationResponse.newBuilder()
          .setNormalizedInput("")
          .setError("Normalization system is not initialized.")
          .build());
    }
    responseObserver.onCompleted();
  }
}
