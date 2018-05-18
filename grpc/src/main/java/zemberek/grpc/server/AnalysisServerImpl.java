package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.List;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.proto.AnalysisRequest;
import zemberek.proto.AnalysisResponse;
import zemberek.proto.AnalysisServiceGrpc.AnalysisServiceImplBase;

public class AnalysisServerImpl extends AnalysisServiceImplBase {
  private final TurkishMorphology morphology;

  public AnalysisServerImpl() {
    morphology = TurkishMorphology.createWithDefaults();
  }

  @Override
  public void analyze(AnalysisRequest request, StreamObserver<AnalysisResponse> responseObserver) {
    List<WordAnalysis> analysisList = morphology.analyzeSentence(request.getInput());
    StringBuilder sb = new StringBuilder("Input: "  + request.getInput());
    for(WordAnalysis wa : analysisList) {
      for (SingleAnalysis sa : wa) {
        sb.append(sa.formatLong());
      }
    }
    responseObserver.onNext(AnalysisResponse.newBuilder()
        .setAnalysis(sb.toString())
        .build());
    responseObserver.onCompleted();
  }
}
