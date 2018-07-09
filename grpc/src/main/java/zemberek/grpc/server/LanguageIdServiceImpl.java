package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import zemberek.langid.LanguageIdentifier;
import zemberek.proto.DetectRequest;
import zemberek.proto.DetectResponse;
import zemberek.proto.LanguageIdServiceGrpc.LanguageIdServiceImplBase;

public class LanguageIdServiceImpl extends LanguageIdServiceImplBase {

  private final LanguageIdentifier languageIdentifier;

  public LanguageIdServiceImpl() throws Exception {
    languageIdentifier = LanguageIdentifier.fromInternalModels();
  }

  @Override
  public void detect(DetectRequest request,
      StreamObserver<DetectResponse> responseObserver) {
    String id = languageIdentifier.identify(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(DetectResponse.newBuilder().setLangId(id).build());
    responseObserver.onCompleted();
  }

  @Override
  public void detectFast(DetectRequest request,
      StreamObserver<DetectResponse> responseObserver) {
    String id = languageIdentifier.identifyFast(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(DetectResponse.newBuilder().setLangId(id).build());
    responseObserver.onCompleted();
  }

}
