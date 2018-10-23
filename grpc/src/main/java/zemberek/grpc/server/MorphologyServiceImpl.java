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
import zemberek.proto.morphology.DictionaryItemProto;
import zemberek.proto.morphology.MorphemeDataProto;
import zemberek.proto.morphology.MorphologyServiceGrpc.MorphologyServiceImplBase;
import zemberek.proto.morphology.SentenceAnalysisRequest;
import zemberek.proto.morphology.SentenceAnalysisProto;
import zemberek.proto.morphology.SentenceWordAnalysisProto;
import zemberek.proto.morphology.SingleAnalysisProto;
import zemberek.proto.morphology.WordAnalysisRequest;
import zemberek.proto.morphology.WordAnalysisProto;


public class MorphologyServiceImpl extends MorphologyServiceImplBase {

  private final TurkishMorphology morphology;

  public MorphologyServiceImpl(ZemberekContext context) {
    morphology = context.morphology;
  }

  @Override
  public void analyzeSentence(SentenceAnalysisRequest request,
      StreamObserver<SentenceAnalysisProto> responseObserver) {
    String sentence = request.getInput();
    SentenceAnalysis a = morphology.analyzeAndDisambiguate(sentence);
    Log.info("Sentence = %s", sentence);
    responseObserver.onNext(toSentenceAnalysis(a, request.getContainAllAnalyses()));
    responseObserver.onCompleted();
  }

  @Override
  public void analyzeWord(
      WordAnalysisRequest request,
      StreamObserver<WordAnalysisProto> responseObserver) {
    String input = request.getInput();
    WordAnalysis a = morphology.analyze(input);
    responseObserver.onNext(toWordAnalysisProto(a));
    responseObserver.onCompleted();
  }

  SentenceAnalysisProto toSentenceAnalysis(SentenceAnalysis sa, boolean allAnalyses) {
    return SentenceAnalysisProto.newBuilder()
        .setInput(sa.getSentence())
        .addAllResults(sa.getWordAnalyses()
            .stream()
            .map(s -> toSentenceWordAnalysisProto(s, allAnalyses))
            .collect(Collectors.toList()))
        .build();
  }

  SentenceWordAnalysisProto toSentenceWordAnalysisProto(SentenceWordAnalysis swa,
      boolean allAnalyses) {
    SentenceWordAnalysisProto.Builder builder = SentenceWordAnalysisProto.newBuilder()
        .setToken(swa.wordAnalysis.getInput())
        .setBest(toSingleAnalysisProto(swa.bestAnalysis));
    if (allAnalyses) {
      builder.setAll(toWordAnalysisProto(swa.wordAnalysis));
    }
    return builder.build();
  }

  WordAnalysisProto toWordAnalysisProto(WordAnalysis analysis) {
    return WordAnalysisProto.newBuilder()
        .addAllAnalyses(analysis
            .stream()
            .map(this::toSingleAnalysisProto)
            .collect(Collectors.toList()))
        .build();
  }

  SingleAnalysisProto toSingleAnalysisProto(SingleAnalysis s) {
    return SingleAnalysisProto.newBuilder()
        .setAnalysis(s.formatLong())
        .setPos(s.getPos().shortForm)
        .setInformal(s.containsInformalMorpheme())
        .setDictionaryItem(toDictionaryItemProto(s.getDictionaryItem()))
        .setRuntime(s.isRuntime())
        .addAllLemmas(s.getLemmas())
        .addAllMorphemes(toMorphemeDataProtoList(s))
        .build();
  }

  DictionaryItemProto toDictionaryItemProto(DictionaryItem dictionaryItem) {
    DictionaryItemProto.Builder builder = DictionaryItemProto.newBuilder()
        .setLemma(dictionaryItem.lemma)
        .setPrimaryPos(dictionaryItem.primaryPos.shortForm);
    if (dictionaryItem.secondaryPos != null &&
        dictionaryItem.secondaryPos != SecondaryPos.None) {
      builder.setSecondaryPos(dictionaryItem.secondaryPos.shortForm);
    }
    return builder.build();
  }

  List<MorphemeDataProto> toMorphemeDataProtoList(SingleAnalysis analysis) {
    List<MorphemeDataProto> result = new ArrayList<>();
    for (MorphemeData m : analysis.getMorphemeDataList()) {
      result.add(MorphemeDataProto.newBuilder()
          .setMorpheme(m.morpheme.id)
          .setSurface(m.surface)
          .build());
    }
    return result;
  }

}
