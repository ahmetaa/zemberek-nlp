package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import zemberek.core.turkish.SecondaryPos;
import zemberek.langid.LanguageIdentifier;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.proto.*;
import zemberek.proto.LanguageIdServiceGrpc.LanguageIdServiceImplBase;
import zemberek.proto.morphology.DictionaryItemProto;
import zemberek.proto.morphology.SentenceAnalysisProto;

import java.util.List;
import java.util.stream.Collectors;

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

  @Override
  public void getScores(LanguageIdRequest request,
                         StreamObserver<LanguageIdScoresResponse> responseObserver) {
    List<LanguageIdentifier.IdResult> idResult = languageIdentifier.getScores(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(toLangIdResult(idResult));
    responseObserver.onCompleted();
  }

  @Override
  public void getScoresFast(LanguageIdRequest request,
                        StreamObserver<LanguageIdScoresResponse> responseObserver) {
    List<LanguageIdentifier.IdResult> idResult = languageIdentifier.getScoresFast(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(toLangIdResult(idResult));
    responseObserver.onCompleted();
  }

  @Override
  public void detectTr(LanguageIdRequest request,
                     StreamObserver<LanguageIdResponse> responseObserver) {
    String id = languageIdentifierTr.identify(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(LanguageIdResponse.newBuilder().setLangId(id).build());
    responseObserver.onCompleted();
  }

  @Override
  public void detectFastTr(LanguageIdRequest request,
                         StreamObserver<LanguageIdResponse> responseObserver) {
    String id = languageIdentifierTr.identifyFast(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(LanguageIdResponse.newBuilder().setLangId(id).build());
    responseObserver.onCompleted();
  }

  @Override
  public void getScoresTr(LanguageIdRequest request,
                        StreamObserver<LanguageIdScoresResponse> responseObserver) {
    List<LanguageIdentifier.IdResult> idResult = languageIdentifierTr.getScores(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(toLangIdResult(idResult));
    responseObserver.onCompleted();
  }

  @Override
  public void getScoresFastTr(LanguageIdRequest request,
                            StreamObserver<LanguageIdScoresResponse> responseObserver) {
    List<LanguageIdentifier.IdResult> idResult = languageIdentifierTr.getScoresFast(request.getInput(), request.getMaxSampleCount());

    responseObserver.onNext(toLangIdResult(idResult));
    responseObserver.onCompleted();
  }

  LanguageIdScoresResponse toLangIdResult(List<LanguageIdentifier.IdResult> idResult) {
    LanguageIdScoresResponse.Builder builder = LanguageIdScoresResponse.newBuilder();
    for (LanguageIdentifier.IdResult item : idResult) {
      builder.addIdResult(IdResult.newBuilder().setId(item.id).setScore(item.score).build());
    }
    return builder.build();
  }

}
