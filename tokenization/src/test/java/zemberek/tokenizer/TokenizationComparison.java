package zemberek.tokenizer;

import com.google.common.base.Stopwatch;
import org.junit.Test;
import zemberek.core.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TokenizationComparison {
    @Test
    public void checkSpeed() throws IOException {

        List<String> lines = Files.readAllLines(
                //Paths.get("/media/depo/data/aaa/corpora/dunya.100k")
                Paths.get("/home/ahmetaa/data/nlp/corpora/dunya.100k")
                //Paths.get("/media/depo/data/aaa/corpora/subtitle-1M")
        );

        Log.info("------- Turkish Tokenizer -------");
        run(TurkishTokenizer.fromInternalModel(), lines);
        Log.info("------- Antlr Lexer Tokenizer -------");
        run(new ZemberekLexer(), lines);
    }

    private void run(Tokenizer tokenizer, List<String> lines) {
        long tokenCount = 0;
        Stopwatch clock = Stopwatch.createStarted();

        for (String line : lines) {
            List<String> tokens = tokenizer.tokenStrings(line);
            tokenCount += tokens.size();
        }
        long elapsed = clock.elapsed(TimeUnit.MILLISECONDS);
        Log.info("Elapsed Time = " + elapsed);
        Log.info("Token Count = " + tokenCount);
        Log.info("Tokenization Speed = %.1f tokens/sec",
                tokenCount * 1000d / elapsed);
    }
}
