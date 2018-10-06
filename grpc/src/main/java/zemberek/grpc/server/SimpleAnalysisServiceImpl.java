package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.proto.simple_analysis.DictionaryItem_P;
import zemberek.proto.simple_analysis.SentenceAnalysis_P;
import zemberek.proto.simple_analysis.SentenceRequest;
import zemberek.proto.simple_analysis.SentenceWordAnalysis_P;
import zemberek.proto.simple_analysis.SimpleAnalysisServiceGrpc.SimpleAnalysisServiceImplBase;
import zemberek.proto.simple_analysis.SingleAnalysis_P;
import zemberek.proto.simple_analysis.WordAnalysis_P;
import zemberek.proto.simple_analysis.WordRequest;


public class SimpleAnalysisServiceImpl extends SimpleAnalysisServiceImplBase {

  private final TurkishMorphology morphology;

  public SimpleAnalysisServiceImpl(ZemberekContext context) {
    morphology = context.morphology;
  }

  @Override
  public void analyzeSentence(SentenceRequest request,
      StreamObserver<SentenceAnalysis_P> responseObserver) {
    String sentence = request.getInput();
    SentenceAnalysis a = morphology.analyzeAndDisambiguate(sentence);
    Log.info("Sentence = %s", sentence);
    responseObserver.onNext(toSentenceAnalysis(a));
    responseObserver.onCompleted();
  }

  @Override
  public void analyzeWord(WordRequest request, StreamObserver<WordAnalysis_P> responseObserver) {
    String input = request.getInput();
    WordAnalysis a = morphology.analyze(input);
    responseObserver.onNext(toWordAnalysisProto(a));
    responseObserver.onCompleted();
  }

  SentenceAnalysis_P toSentenceAnalysis(SentenceAnalysis sa) {
    return SentenceAnalysis_P.newBuilder()
        .setInput(sa.getSentence())
        .addAllResults(sa.getWordAnalyses()
            .stream()
            .map(this::toSentenceWordAnalysisProto)
            .collect(Collectors.toList()))
        .build();
  }

  SentenceWordAnalysis_P toSentenceWordAnalysisProto(SentenceWordAnalysis swa) {
    return SentenceWordAnalysis_P.newBuilder()
        .setToken(swa.wordAnalysis.getInput())
        .setBest(toSingleAnalysisProto(swa.bestAnalysis))
        .setAll(toWordAnalysisProto(swa.wordAnalysis))
        .build();
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
        .build();
  }

  DictionaryItem_P toDictionaryItemProto(DictionaryItem dictionaryItem) {
    DictionaryItem_P.Builder builder = DictionaryItem_P.newBuilder()
        .setLemma(dictionaryItem.lemma)
        .setPos(dictionaryItem.primaryPos.shortForm);
    if (dictionaryItem.secondaryPos != null &&
        dictionaryItem.secondaryPos != SecondaryPos.None) {
      builder.setPos2(dictionaryItem.secondaryPos.shortForm);
    }
    return builder.build();
  }
}
