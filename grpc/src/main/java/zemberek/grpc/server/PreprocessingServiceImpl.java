package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.proto.PreprocessingServiceGrpc.PreprocessingServiceImplBase;
import zemberek.proto.TokenizationRequest;
import zemberek.proto.TokenizationResponse;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class PreprocessingServiceImpl extends PreprocessingServiceImplBase {

  private final TurkishTokenizer tokenizer;

  public PreprocessingServiceImpl() {
    tokenizer = TurkishTokenizer.DEFAULT;
  }

  public void tokenize(TokenizationRequest request,
      StreamObserver<TokenizationResponse> responseObserver) {
    List<zemberek.proto.Token> tokens =
        tokenizer.tokenize(request.getInput())
            .stream()
            .map(token ->
                zemberek.proto.Token.newBuilder()
                    .setToken(token.getText())
                    .setType(TurkishLexer.VOCABULARY.getDisplayName(token.getType()))
                    .build())
            .collect(Collectors.toList());
    responseObserver.onNext(TokenizationResponse.newBuilder()
        .addAllTokens(tokens)
        .build()
    );
    responseObserver.onCompleted();
  }
}
