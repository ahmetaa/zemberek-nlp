package zemberek.morphology.apps;

import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.SimpleParser;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ParseConsole {

    public void doit() throws IOException {
        SimpleParser parser = createSimpleParser();
        String input;
        System.out.println("Enter word:");
        Scanner sc = new Scanner(System.in);
        input = sc.nextLine();
        while (!input.equals("exit") && !input.equals("quit")) {
            List<MorphParse> tokens = parser.parse(input);
            if (tokens.size() == 0) {
                System.out.println("cannot be parsed");
                parser.dump(input);
            } else {
                for (MorphParse token : tokens) {
                    printMorphParse(token);

                }
            }
            input = sc.nextLine();
        }
    }

    protected void printMorphParse(MorphParse token) {
        //System.out.println(token.asParseString());
        //System.out.println(new MorphParse(token).formatOflazer());
        //System.out.println(new MorphParse(token).formatNoSurface());
        System.out.println(token.formatLong());
        //System.out.println(token.formatNoEmpty());
//                    System.out.println(new MorphParse(token).formatNoEmpty());
    }

    protected SimpleParser createSimpleParser() throws IOException {
        DynamicLexiconGraph graph = createLexiconGraph();
        //graph.stats();
        return new SimpleParser(graph);
    }

    protected DynamicLexiconGraph createLexiconGraph() throws IOException {
        SuffixProvider suffixProvider = new TurkishSuffixes();
        RootLexicon items = createRootLexicon(suffixProvider);
        DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
        graph.addDictionaryItems(items);
        return graph;
    }

    protected RootLexicon createRootLexicon(SuffixProvider suffixProvider) throws IOException {
        return TurkishDictionaryLoader.loadDefaultDictionaries(suffixProvider);
    }

    public static void main(String[] args) throws IOException {
        // to test the devl lexicon, use ParseConsoleTest
        new ParseConsole().doit();
    }
}
