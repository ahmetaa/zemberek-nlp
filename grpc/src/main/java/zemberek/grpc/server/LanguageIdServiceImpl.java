package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import zemberek.langid.LanguageIdentifier;
import zemberek.proto.LanguageIdRequest;
import zemberek.proto.LanguageIdResponse;
import zemberek.proto.LanguageIdServiceGrpc.LanguageIdServiceImplBase;

public class LanguageIdServiceImpl extends LanguageIdServiceImplBase {

  private final LanguageIdentifier languageIdentifier;

  public LanguageIdServiceImpl() throws Exception {
    languageIdentifier = LanguageIdentifier.fromInternalModels();
  }

  @Override
  public void detect(LanguageIdRequest request,
      StreamObserver<LanguageIdResponse> responseObserver) {
    String id = languageIdentifier.identify(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(LanguageIdResponse.newBuilder().setLangId(id).build());
    responseObserver.onCompleted();
  }

  @Override
  public void detectFast(LanguageIdRequest request,
      StreamObserver<LanguageIdResponse> responseObserver) {
    String id = languageIdentifier.identifyFast(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(LanguageIdResponse.newBuilder().setLangId(id).build());
    responseObserver.onCompleted();
  }

}
