package zemberek.morphology.apps;

import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.WordParser;
import zemberek.morphology.parser.tr.TurkishWordParserGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ParseConsole {

    public void run(TurkishWordParserGenerator parser) throws IOException {
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

    public static void main(String[] args) throws IOException {
        // to test the development lexicon, use ParseConsoleTest
        new ParseConsole().run(TurkishWordParserGenerator.createWithDefaults());
    }
}
