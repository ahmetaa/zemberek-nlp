package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.proto.PreprocessingServiceGrpc.PreprocessingServiceImplBase;
import zemberek.proto.SentenceExtractionRequest;
import zemberek.proto.SentenceExtractionResponse;
import zemberek.proto.TokenProto;
import zemberek.proto.TokenizationRequest;
import zemberek.proto.TokenizationResponse;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class PreprocessingServiceImpl extends PreprocessingServiceImplBase {

  private final TurkishTokenizer tokenizer;
  private final TurkishSentenceExtractor defaultExtractor;

  // this extractor does not split sentences in double qouotes.
  private final TurkishSentenceExtractor doubleQuoteIgnoreExtractor;

  public PreprocessingServiceImpl() {
    tokenizer = TurkishTokenizer.DEFAULT;
    defaultExtractor = TurkishSentenceExtractor.DEFAULT;
    doubleQuoteIgnoreExtractor = TurkishSentenceExtractor
        .builder()
        .doNotSplitInDoubleQuotes()
        .build();
  }

  public void tokenize(TokenizationRequest request,
      StreamObserver<TokenizationResponse> responseObserver) {
    List<TokenProto> tokens =
        tokenizer.tokenize(request.getInput())
            .stream()
            .map(token -> build(request, token))
            .collect(Collectors.toList());
    responseObserver.onNext(TokenizationResponse.newBuilder()
        .addAllTokens(tokens)
        .build());
    responseObserver.onCompleted();
  }

  private static TokenProto build(TokenizationRequest request, Token token) {
    TokenProto.Builder builder = TokenProto.newBuilder().setToken(token.getText())
        .setType(token.getType().name());
    if (request.getIncludeTokenBoundaries()) {
      builder.setStart(token.getStart())
          .setEnd(token.getEnd());
    }
    return builder.build();
  }

  public void extractSentences(SentenceExtractionRequest request,
      StreamObserver<SentenceExtractionResponse> responseObserver) {
    TurkishSentenceExtractor extractor = request.getDoNotSplitInDoubleQuotes() ?
        defaultExtractor : doubleQuoteIgnoreExtractor;
    responseObserver.onNext(SentenceExtractionResponse.newBuilder()
        .addAllSentences(extractor.fromDocument(request.getDocument()))
        .build());
    responseObserver.onCompleted();
  }
}
