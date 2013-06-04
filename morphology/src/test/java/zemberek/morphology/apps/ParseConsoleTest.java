package zemberek.morphology.apps;

import com.google.common.io.Resources;
import org.junit.Ignore;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

import java.io.File;
import java.io.IOException;

/**
 * Uses a devl lexicon
 */
@Ignore
public class ParseConsoleTest {

    public static void main(String[] args) throws IOException {
        new DevlParseConsole().doit();
    }

    private static class DevlParseConsole extends ParseConsole {
        @Override
        protected RootLexicon createRootLexicon(SuffixProvider suffixProvider) throws IOException {
            return new TurkishDictionaryLoader(suffixProvider).load(new File(Resources.getResource("dev-lexicon.txt").getFile()));
        }
    }
}
