package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import zemberek.langid.LanguageIdentifier;
import zemberek.proto.*;
import zemberek.proto.LanguageIdServiceGrpc.LanguageIdServiceImplBase;

import java.util.List;

public class LanguageIdServiceImpl extends LanguageIdServiceImplBase {

  private final LanguageIdentifier languageIdentifier;
  private final LanguageIdentifier languageIdentifierTr;

  public LanguageIdServiceImpl() throws Exception {
    languageIdentifier = LanguageIdentifier.fromInternalModels();
    languageIdentifierTr = LanguageIdentifier.fromInternalModelGroup("tr_group");
  }

  @Override
  public void detect(LanguageIdRequest request,
      StreamObserver<LanguageIdResponse> responseObserver) {
    LanguageIdentifier identifier = request.getTrGroup() ? languageIdentifierTr : languageIdentifier;

    String id = identifier.identify(request.getInput(), request.getMaxSampleCount());

    LanguageIdResponse.Builder builder = LanguageIdResponse.newBuilder().setLangId(id);
    if(request.getIncludeScores()){
      List<LanguageIdentifier.IdResult> scores = identifier.getScores(request.getInput(), request.getMaxSampleCount());
      for (LanguageIdentifier.IdResult item : scores) {
        builder.addIdResult(IdResult.newBuilder().setId(item.id).setScore(item.score).build());
      }
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void detectFast(LanguageIdRequest request,
      StreamObserver<LanguageIdResponse> responseObserver) {
    LanguageIdentifier identifier = request.getTrGroup() ? languageIdentifierTr : languageIdentifier;

    String id = identifier.identifyFast(request.getInput(), request.getMaxSampleCount());

    LanguageIdResponse.Builder builder = LanguageIdResponse.newBuilder().setLangId(id);
    if(request.getIncludeScores()){
      List<LanguageIdentifier.IdResult> scores = identifier.getScoresFast(request.getInput(), request.getMaxSampleCount());
      for (LanguageIdentifier.IdResult item : scores) {
        builder.addIdResult(IdResult.newBuilder().setId(item.id).setScore(item.score).build());
      }
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

}
