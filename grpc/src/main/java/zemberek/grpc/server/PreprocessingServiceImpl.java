package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.proto.PreprocessingServiceGrpc.PreprocessingServiceImplBase;
import zemberek.proto.SentenceExtractionRequest;
import zemberek.proto.SentenceExtractionResponse;
import zemberek.proto.Token.Builder;
import zemberek.proto.TokenizationRequest;
import zemberek.proto.TokenizationResponse;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class PreprocessingServiceImpl extends PreprocessingServiceImplBase {

  private final TurkishTokenizer tokenizer;
  private final TurkishSentenceExtractor extractor;

  public PreprocessingServiceImpl() {
    tokenizer = TurkishTokenizer.DEFAULT;
    extractor = TurkishSentenceExtractor.DEFAULT;
  }

  public void tokenize(TokenizationRequest request,
      StreamObserver<TokenizationResponse> responseObserver) {
    List<zemberek.proto.Token> tokens =
        tokenizer.tokenize(request.getInput())
            .stream()
            .map(token -> build(request, token))
            .collect(Collectors.toList());
    responseObserver.onNext(TokenizationResponse.newBuilder()
        .addAllTokens(tokens)
        .build()
    );
    responseObserver.onCompleted();
  }

  private static zemberek.proto.Token build(TokenizationRequest request, Token token) {
    Builder builder = zemberek.proto.Token.newBuilder().setToken(token.getText())
        .setType(TurkishLexer.VOCABULARY.getDisplayName(token.getType()));
    if (request.getIncludeTokenBoundaries()) {
      builder.setStart(token.getStartIndex())
          .setEnd(token.getStopIndex());
    }
    return builder.build();
  }

  public void extractSentences(SentenceExtractionRequest request,
      StreamObserver<SentenceExtractionResponse> responseObserver) {
    responseObserver.onNext(SentenceExtractionResponse.newBuilder()
        .addAllSentences(extractor.fromDocument(request.getDocument()))
        .build());
    responseObserver.onCompleted();
  }
}
