package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.proto.morphology.DictionaryItem_P;
import zemberek.proto.morphology.MorphemeData_P;
import zemberek.proto.morphology.MorphologyServiceGrpc.MorphologyServiceImplBase;
import zemberek.proto.morphology.SentenceAnalysisRequest;
import zemberek.proto.morphology.SentenceAnalysis_P;
import zemberek.proto.morphology.SentenceWordAnalysis_P;
import zemberek.proto.morphology.SingleAnalysis_P;
import zemberek.proto.morphology.WordAnalysisRequest;
import zemberek.proto.morphology.WordAnalysis_P;


public class MorphologyServiceImpl extends MorphologyServiceImplBase {

  private final TurkishMorphology morphology;

  public MorphologyServiceImpl(ZemberekContext context) {
    morphology = context.morphology;
  }

  @Override
  public void analyzeSentence(SentenceAnalysisRequest request,
      StreamObserver<SentenceAnalysis_P> responseObserver) {
    String sentence = request.getInput();
    SentenceAnalysis a = morphology.analyzeAndDisambiguate(sentence);
    Log.info("Sentence = %s", sentence);
    responseObserver.onNext(toSentenceAnalysis(a, request.getContainAllAnalyses()));
    responseObserver.onCompleted();
  }

  @Override
  public void analyzeWord(
      WordAnalysisRequest request,
      StreamObserver<WordAnalysis_P> responseObserver) {
    String input = request.getInput();
    WordAnalysis a = morphology.analyze(input);
    responseObserver.onNext(toWordAnalysisProto(a));
    responseObserver.onCompleted();
  }

  SentenceAnalysis_P toSentenceAnalysis(SentenceAnalysis sa, boolean allAnalyses) {
    return SentenceAnalysis_P.newBuilder()
        .setInput(sa.getSentence())
        .addAllResults(sa.getWordAnalyses()
            .stream()
            .map(s -> toSentenceWordAnalysisProto(s, allAnalyses))
            .collect(Collectors.toList()))
        .build();
  }

  SentenceWordAnalysis_P toSentenceWordAnalysisProto(SentenceWordAnalysis swa,
      boolean allAnalyses) {
    SentenceWordAnalysis_P.Builder builder = SentenceWordAnalysis_P.newBuilder()
        .setToken(swa.wordAnalysis.getInput())
        .setBest(toSingleAnalysisProto(swa.bestAnalysis));
    if (allAnalyses) {
      builder.setAll(toWordAnalysisProto(swa.wordAnalysis));
    }
    return builder.build();
  }

  WordAnalysis_P toWordAnalysisProto(WordAnalysis analysis) {
    return WordAnalysis_P.newBuilder()
        .addAllAnalyses(analysis
            .stream()
            .map(this::toSingleAnalysisProto)
            .collect(Collectors.toList()))
        .build();
  }

  SingleAnalysis_P toSingleAnalysisProto(SingleAnalysis s) {
    return SingleAnalysis_P.newBuilder()
        .setAnalysis(s.formatLong())
        .setPos(s.getPos().shortForm)
        .setInformal(s.containsInformalMorpheme())
        .setDictionaryItem(toDictionaryItemProto(s.getDictionaryItem()))
        .setRuntime(s.isRuntime())
        .addAllLemmas(s.getLemmas())
        .addAllMorphemes(toMorphemeDataProtoList(s))
        .build();
  }

  DictionaryItem_P toDictionaryItemProto(DictionaryItem dictionaryItem) {
    DictionaryItem_P.Builder builder = DictionaryItem_P.newBuilder()
        .setLemma(dictionaryItem.lemma)
        .setPrimaryPos(dictionaryItem.primaryPos.shortForm);
    if (dictionaryItem.secondaryPos != null &&
        dictionaryItem.secondaryPos != SecondaryPos.None) {
      builder.setSecondaryPos(dictionaryItem.secondaryPos.shortForm);
    }
    return builder.build();
  }

  List<MorphemeData_P> toMorphemeDataProtoList(SingleAnalysis analysis) {
    List<MorphemeData_P> result = new ArrayList<>();
    for (MorphemeData m : analysis.getMorphemeDataList()) {
      result.add(MorphemeData_P.newBuilder()
          .setMorpheme(m.morpheme.id)
          .setSurface(m.surface)
          .build());
    }
    return result;
  }

}
