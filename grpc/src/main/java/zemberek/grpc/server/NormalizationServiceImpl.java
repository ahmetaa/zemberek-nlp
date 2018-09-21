package zemberek.grpc.server;

import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.List;
import org.antlr.v4.runtime.Token;
import zemberek.normalization.TurkishSpellChecker;
import zemberek.proto.NormalizationRequest;
import zemberek.proto.NormalizationResponse;
import zemberek.proto.NormalizationServiceGrpc.NormalizationServiceImplBase;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

public class NormalizationServiceImpl extends NormalizationServiceImplBase {
  private final TurkishSpellChecker spellChecker;
  private final TurkishTokenizer tokenizer;

  public NormalizationServiceImpl(ZemberekContext context) throws IOException {
    spellChecker = new TurkishSpellChecker(context.morphology);
    tokenizer = context.tokenizer;
  }

  @Override
  public void normalize(NormalizationRequest request,
      StreamObserver<NormalizationResponse> responseObserver) {
    StringBuilder output = new StringBuilder();
    for (Token token : tokenizer.tokenize(request.getInput())) {
      String text = token.getText();
      if (analyzeToken(token) && !spellChecker.check(text)) {
        List<String> strings = spellChecker.suggestForWord(token.getText());
        if (!strings.isEmpty()) {
          String suggestion = strings.get(0);
          output.append(suggestion);
        } else {
          output.append(text);
        }
      } else {
        output.append(text);
      }
    }
    responseObserver.onNext(NormalizationResponse.newBuilder()
        .setNormalizedInput(output.toString())
        .build());
    responseObserver.onCompleted();
  }

  static boolean analyzeToken(Token token) {
    return token.getType() != TurkishLexer.NewLine
        && token.getType() != TurkishLexer.SpaceTab
        && token.getType() != TurkishLexer.UnknownWord
        && token.getType() != TurkishLexer.RomanNumeral
        && token.getType() != TurkishLexer.Unknown;
  }
}
