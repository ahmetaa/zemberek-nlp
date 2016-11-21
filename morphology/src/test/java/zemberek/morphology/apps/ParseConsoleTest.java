package zemberek.morphology.apps;

import com.google.common.io.Resources;
import org.junit.Ignore;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.File;
import java.io.IOException;

/**
 * Uses a devl lexicon
 */
@Ignore
public class ParseConsoleTest {

    public static void main(String[] args) throws IOException {
        new ParseConsole().run(
                TurkishMorphology
                        .builder()
                        .addTextDictFiles(new File(Resources.getResource("dev-lexicon.txt").getFile()))
                        .build()
        );
    }
}
