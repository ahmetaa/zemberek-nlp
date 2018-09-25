package zemberek.grpc.server;


import io.grpc.stub.StreamObserver;
import zemberek.morphology.TurkishMorphology;
import zemberek.proto.lemmatization.LemmatizationServiceGrpc.LemmatizationServiceImplBase;
import zemberek.proto.lemmatization.SentenceRequest;
import zemberek.proto.lemmatization.SentenceResponse;
import zemberek.proto.lemmatization.WordRequest;
import zemberek.proto.lemmatization.WordResponse;

public class LemmatizationServiceImpl extends LemmatizationServiceImplBase {

  private final TurkishMorphology morphology;

  public LemmatizationServiceImpl(ZemberekContext context) {
    morphology = context.morphology;
  }

  @Override
  public void lemmatizeSentence(SentenceRequest request,
      StreamObserver<SentenceResponse> responseObserver) {
    super.lemmatizeSentence(request, responseObserver);
  }

  @Override
  public void lemmatizeWord(WordRequest request, StreamObserver<WordResponse> responseObserver) {
    super.lemmatizeWord(request, responseObserver);
  }
}
