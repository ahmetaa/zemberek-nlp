package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import zemberek.langid.LanguageIdentifier;
import zemberek.proto.LangIdRequest;
import zemberek.proto.LangIdResponse;
import zemberek.proto.LanguageIdServiceGrpc.LanguageIdServiceImplBase;

public class LanguageIdServiceImpl extends LanguageIdServiceImplBase {

  private final LanguageIdentifier languageIdentifier;

  public LanguageIdServiceImpl() throws Exception {
    languageIdentifier = LanguageIdentifier.fromInternalModels();
  }

  @Override
  public void detectLanguage(LangIdRequest request,
      StreamObserver<LangIdResponse> responseObserver) {
    String id = languageIdentifier.identify(request.getInput());
    responseObserver.onNext(LangIdResponse.newBuilder().setLangId(id).build());
    responseObserver.onCompleted();
  }
}
