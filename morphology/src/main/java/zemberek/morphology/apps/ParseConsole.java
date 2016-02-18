package zemberek.morphology.apps;

import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.WordParser;
import zemberek.morphology.parser.tr.TurkishWordParserGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ParseConsole {

    public void doit() throws IOException {
        TurkishWordParserGenerator parser = TurkishWordParserGenerator.createWithDefaults();
        String input;
        System.out.println("Enter word:");
        Scanner sc = new Scanner(System.in);
        input = sc.nextLine();
        while (!input.equals("exit") && !input.equals("quit")) {

            List<MorphParse> tokens = parser.parse(input);
            if (tokens.size() == 0) {
                System.out.println("cannot be parsed");
                if (parser.getParser() instanceof WordParser) {
                    ((WordParser) parser.getParser()).dump(input);
                }
            } else {
                tokens.forEach(this::printMorphParse);
            }
            input = sc.nextLine();
        }
    }

    protected void printMorphParse(MorphParse token) {
        String runtime = token.dictionaryItem.hasAttribute(RootAttribute.Runtime) ? " [Not in dictionary]" : "";
        System.out.println(token.formatLong() + runtime);
    }

    protected RootLexicon createRootLexicon(SuffixProvider suffixProvider) throws IOException {
        return TurkishDictionaryLoader.loadDefaultDictionaries(suffixProvider);
    }

    public static void main(String[] args) throws IOException {
        // to test the development lexicon, use ParseConsoleTest
        new ParseConsole().doit();
    }
}
