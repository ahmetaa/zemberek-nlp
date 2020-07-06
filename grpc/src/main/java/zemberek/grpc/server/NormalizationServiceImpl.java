package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.io.IOException;
import zemberek.core.logging.Log;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.proto.NormalizationRequest;
import zemberek.proto.NormalizationResponse;
import zemberek.proto.NormalizationServiceGrpc.NormalizationServiceImplBase;

public class NormalizationServiceImpl extends NormalizationServiceImplBase {

  private ZemberekContext context;

  private TurkishSentenceNormalizer sentenceNormalizer;

  public NormalizationServiceImpl(ZemberekContext context) throws IOException {
    this.context = context;
    if (context.configuration != null && context.configuration.normalizationPathsAvailable()) {
      sentenceNormalizer = new TurkishSentenceNormalizer(
          context.morphology,
          context.configuration.normalizationDataRoot,
          context.configuration.normalizationLmPath);
    } else {
      Log.warn("Normalization paths are not available. Normalization service is down.");
    }
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
