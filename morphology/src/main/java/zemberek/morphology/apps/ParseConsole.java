package zemberek.morphology.apps;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ParseConsole {

    public void run(TurkishMorphology parser) throws IOException {
        String input;
        System.out.println("Enter word:");
        Scanner sc = new Scanner(System.in);
        input = sc.nextLine();
        while (!input.equals("exit") && !input.equals("quit")) {

            List<WordAnalysis> tokens = parser.analyze(input);
            if (tokens.size() == 0 || (tokens.size() == 1 && tokens.get(0).dictionaryItem.primaryPos == PrimaryPos.Unknown)) {
                System.out.println("cannot be parsed");
                parser.getWordAnalyzer().dump(input);
            } else {
                tokens.forEach(this::printMorphParse);
            }
            input = sc.nextLine();
        }
    }

    protected void printMorphParse(WordAnalysis token) {
        String runtime = token.dictionaryItem.hasAttribute(RootAttribute.Runtime) ? " [Not in dictionary]" : "";
        System.out.println(token.formatLong() + runtime);
        System.out.println(token.formatOflazer() + runtime);
    }

    public static RootLexicon addTextDictionaryResources(SuffixProvider suffixProvider, String... resources) throws IOException {
        RootLexicon lexicon = new RootLexicon();
        Log.info("Dictionaries :%s", String.join(", ", Arrays.asList(resources)));
        List<String> lines = new ArrayList<>();
        for (String resource : resources) {
            lines.addAll(Resources.readLines(Resources.getResource(resource), Charsets.UTF_8));
        }
        lexicon.addAll(new TurkishDictionaryLoader(suffixProvider).load(lines));
        Log.info("Lexicon Generated.");
        return lexicon;
    }


    public static void main(String[] args) throws IOException {
        // to test the development lexicon, use ParseConsoleTest
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        //morphology.getGraph().stats();
        new ParseConsole().run(morphology);
    }
}
