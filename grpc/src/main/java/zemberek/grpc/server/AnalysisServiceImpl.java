package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.proto.AnalysisRequest;
import zemberek.proto.AnalysisResponse;
import zemberek.proto.AnalysisServiceGrpc.AnalysisServiceImplBase;

public class AnalysisServiceImpl extends AnalysisServiceImplBase {

  private final TurkishMorphology morphology;

  public AnalysisServiceImpl(ZemberekContext context) throws IOException {
    morphology = context.morphology;
  }

  @Override
  public void analyze(AnalysisRequest request, StreamObserver<AnalysisResponse> responseObserver) {
    List<WordAnalysis> analysisList = morphology.analyzeSentence(request.getInput());
    List<zemberek.proto.WordAnalysis> wordAnalysisList = new ArrayList<>();
    for (WordAnalysis wa : analysisList) {
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
    zemberek.proto.SingleAnalysis.Builder builder = zemberek.proto.SingleAnalysis.newBuilder()
        .setDictionaryItem(toDictionaryItemProto(analysis.getDictionaryItem()))
        .addAllMorphemeData(analysis.getMorphemeDataList()
            .stream()
            .map(this::toMorphemeDataProto)
            .collect(Collectors.toList()));
    return builder.build();
  }

  zemberek.proto.MorphemeData toMorphemeDataProto(MorphemeData morphemeData) {
    Log.info("MorphemeData: " + morphemeData);
    zemberek.proto.MorphemeData.Builder builder = zemberek.proto.MorphemeData.newBuilder()
        .setMorpheme(toMorphemeProto(morphemeData.morpheme))
        .setSurface(morphemeData.surface);
    return builder.build();
  }

  zemberek.proto.Morpheme toMorphemeProto(Morpheme morpheme) {
    zemberek.proto.Morpheme.Builder builder = zemberek.proto.Morpheme.newBuilder()
        .setId(morpheme.id)
        .setName(morpheme.name)
        .setDerivational(morpheme.derivational);
    if (morpheme.pos != null) {
      builder.setPrimaryPos(morpheme.pos.shortForm);
    }
    return builder.build();
    // TODO Add other fields.
  }

  zemberek.proto.DictionaryItem toDictionaryItemProto(DictionaryItem dictionaryItem) {
    zemberek.proto.DictionaryItem.Builder builder = zemberek.proto.DictionaryItem.newBuilder()
        .setLemma(dictionaryItem.lemma)
        .setPrimaryPos(dictionaryItem.primaryPos.shortForm);
    if (dictionaryItem.secondaryPos != null) {
      builder.setSecondaryPos(dictionaryItem.secondaryPos.shortForm);
    }
    return builder.build();
  }
}
