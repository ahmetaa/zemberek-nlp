package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.proto.AnalysisRequest;
import zemberek.proto.AnalysisResponse;
import zemberek.proto.AnalysisServiceGrpc.AnalysisServiceImplBase;
import zemberek.morphology.morphotactics.Morpheme;

public class AnalysisServerImpl extends AnalysisServiceImplBase {
  private final TurkishMorphology morphology;

  public AnalysisServerImpl() {
    morphology = TurkishMorphology.createWithDefaults();
  }

  @Override
  public void analyze(AnalysisRequest request, StreamObserver<AnalysisResponse> responseObserver) {
    List<WordAnalysis> analysisList = morphology.analyzeSentence(request.getInput());
    List<zemberek.proto.WordAnalysis> wordAnalysisList = new ArrayList<>();
    for(WordAnalysis wa : analysisList) {
      zemberek.proto.WordAnalysis wordAnalysis = zemberek.proto.WordAnalysis.newBuilder()
          .addAllSingleAnalysis(wa.getAnalysisResults()
              .stream()
              .map(this::toSingleAnalysisProto)
              .collect(Collectors.toList()))
          .build();
      wordAnalysisList.add(wordAnalysis);
    }
    responseObserver.onNext(AnalysisResponse.newBuilder()
        .addAllWordAnalysis(wordAnalysisList)
        .build());
    responseObserver.onCompleted();
  }

  // TODO use different names for protos.
  zemberek.proto.SingleAnalysis toSingleAnalysisProto(SingleAnalysis analysis) {
    zemberek.proto.SingleAnalysis.Builder builder = zemberek.proto.SingleAnalysis.newBuilder();
    builder.addAllMorphemeData(analysis.getMorphemeDataList()
        .stream()
        .map(this::tomorphemeDataProto)
        .collect(Collectors.toList()));
    return  builder.build();
  }

  zemberek.proto.MorphemeData tomorphemeDataProto(MorphemeData morphemeData) {
    zemberek.proto.MorphemeData.Builder builder =  zemberek.proto.MorphemeData.newBuilder();
    builder.setMorpheme(tomorphemeProto(morphemeData.morpheme))
        .setSurface(morphemeData.surface);
    return  builder.build();
  }

  zemberek.proto.Morpheme tomorphemeProto(Morpheme morpheme) {
    zemberek.proto.Morpheme.Builder builder = zemberek.proto.Morpheme.newBuilder();
    return builder.setId(morpheme.id)
        .setName(morpheme.name)
        .setDerivational(morpheme.derivational)
        .build();
        // TODO Add other fields.
  }
}
