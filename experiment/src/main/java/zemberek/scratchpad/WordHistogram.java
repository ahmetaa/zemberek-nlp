package zemberek.scratchpad;

import org.antlr.v4.runtime.Token;
import zemberek.core.collections.Histogram;
import zemberek.core.io.Strings;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.corpus.WebCorpus;
import zemberek.corpus.WebDocument;
import zemberek.deasciifier.Deasciifier;
import zemberek.langid.LanguageIdentifier;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.analysis.tr.TurkishSentenceAnalyzer;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WordHistogram {

    public static void convert(Path input, Path output) throws IOException {
        List<String> lines = Files.readAllLines(
                input, Charset.forName("ISO-8859-9"));
        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    static List<String> loadChunks(Path input) throws IOException {

        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        List<String> result = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String reducedLine = line
                    .toLowerCase(Turkish.LOCALE)
                    .replaceAll("[^a-zöçşğıüâ]", "");
            if (reducedLine.length() < 15) {
                String e = sb.toString();
                if (e.length() > 0) {
                    result.add(e);
                }
                sb = new StringBuilder();
                continue;
            }
            String str = line.replaceAll("-$", "").replaceAll("\\s+", " ");
            if (!str.contains("/SAYI") && !str.contains("/ SAYI")) {
                sb.append(str).append(" ");
            }
        }
        if (sb.length() < 20) {
            result.add(sb.toString());
        }
        return result;
    }

    static List<String> removeNonTurkish(Path input) throws IOException {
        LanguageIdentifier identifier = LanguageIdentifier.fromInternalModels();
        List<String> chunks = Files.readAllLines(input, StandardCharsets.UTF_8);

        return chunks.stream()
                .filter(s -> identifier.identifyFast(s, 200).equalsIgnoreCase("tr"))
                .collect(Collectors.toList());
    }

    static List<String> deascify(Path input) throws IOException {
        List<String> chunks = Files.readAllLines(input, StandardCharsets.UTF_8);
        List<String> result = new ArrayList<>();
        TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
        for (String chunk : chunks) {

            List<String> words = tokenizer.tokenizeToStrings(chunk);
            String tokenStr = String.join(" ", words);

            String withoutSpaces = chunk.replaceAll("\\s+", "");
            String turkishChrs = chunk.replaceAll("[^çÇöÖğĞüÜıİşŞâî]", "");
            double ratio = turkishChrs.length() * 1d / withoutSpaces.length();
            if (ratio < 0.01) {
                result.add(new Deasciifier(tokenStr).convertToTurkish());
            } else {
                result.add(chunk);
            }
        }
        return result;
    }

    private static List<String> getParagraphsFromCorpus(Path input) throws IOException {
        WebCorpus corpus = new WebCorpus("a", "a");
        corpus.addDocuments(WebCorpus.loadDocuments(input));
        Set<Long> contentHash = new HashSet<>();

        List<String> paragraphs = new ArrayList<>(100000);
        for (WebDocument document : corpus.getDocuments()) {
            Long hash = document.getHash();
            if (contentHash.contains(hash)) {
                continue;
            }
            contentHash.add(hash);
            paragraphs.add(document.getContentAsString());
        }
        return paragraphs;
    }

    static void stats(List<String> paragraphs) {
        int paragraphCounter = 0;
        int sentenceCounter = 0;
        int tokenCounter = 0;
        int tokenNoPunctCounter = 0;
        int tokenWordCounter = 0;

        for (String paragraph : paragraphs) {
            List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraph(paragraph);
            sentenceCounter += sentences.size();
            for (String sentence : sentences) {
                List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
                for (Token token : tokens) {

                    if (token.getType() != TurkishLexer.Punctuation) {
                        tokenNoPunctCounter++;
                    }

                    if (token.getType() == TurkishLexer.PercentNumeral ||
                            token.getType() == TurkishLexer.Number ||
                            token.getType() == TurkishLexer.Punctuation ||
                            token.getType() == TurkishLexer.RomanNumeral ||
                            token.getType() == TurkishLexer.Email ||
                            token.getType() == TurkishLexer.HashTag ||
                            token.getType() == TurkishLexer.Emoticon ||
                            token.getType() == TurkishLexer.Time ||
                            token.getType() == TurkishLexer.Date ||
                            token.getType() == TurkishLexer.URL ||
                            token.getType() == TurkishLexer.UnknownWord ||
                            token.getType() == TurkishLexer.Unknown) {
                        tokenCounter++;
                    } else {
                        tokenCounter++;
                        tokenWordCounter++;
                    }
                }

            }
            paragraphCounter++;
            if (paragraphCounter % 1000 == 0) {
                System.out.println(paragraphCounter + " of " + paragraphs.size());
            }
        }
        System.out.println("sentenceCounter = " + sentenceCounter);
        System.out.println("tokenCounter = " + tokenCounter);
        System.out.println("tokenNoPunctCounter = " + tokenNoPunctCounter);
        System.out.println("tokenWordCounter = " + tokenWordCounter);
    }

    static void generateHistograms(List<String> paragraphs, Path outRoot) throws IOException {

        TurkishMorphology morphology = TurkishMorphology.builder()
                .addDefaultDictionaries()
                .cacheParameters(75_000, 150_000)
                .build();
        TurkishSentenceAnalyzer analyzer = new TurkishSentenceAnalyzer(morphology, new Z3MarkovModelDisambiguator());

        Histogram<String> roots = new Histogram<>(1000_000);
        Histogram<String> words = new Histogram<>(1000_000);

        int paragraphCounter = 0;
        int sentenceCounter = 0;
        int tokenCounter = 0;

        for (String paragraph : paragraphs) {
            List<String> sentences = TurkishSentenceExtractor.DEFAULT.fromParagraph(paragraph);
            sentenceCounter += sentences.size();
            for (String sentence : sentences) {
                List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
                tokenCounter += tokens.size();

                SentenceAnalysis analysis = analyzer.analyze(sentence);
                analyzer.disambiguate(analysis);
                for (SentenceAnalysis.Entry e : analysis) {
                    WordAnalysis best = e.parses.get(0);
                    if (best.getPos() == PrimaryPos.Numeral ||
                            best.getPos() == PrimaryPos.Punctuation) {
                        continue;
                    }
                    if (best.isUnknown()) {
                        continue;
                    }
                    if (best.isRuntime() &&
                            !Strings.containsNone(e.input, "01234567890")) {
                        continue;
                    }
                    List<String> lemmas = best.getLemmas();
                    if (lemmas.size() == 0) {
                        continue;
                    }

                    roots.add(best.getDictionaryItem().lemma);

                    String w = e.input;
                    if (best.getDictionaryItem().secondaryPos != SecondaryPos.ProperNoun) {
                        w = w.toLowerCase(Turkish.LOCALE);
                    } else {
                        w = Turkish.capitalize(w);
                    }
                    words.add(w);
                }
            }
            paragraphCounter++;
            if (paragraphCounter % 1000 == 0) {
                System.out.println(paragraphCounter + " of " + paragraphs.size());
            }
        }

        System.out.println("tokenCounter = " + tokenCounter);
        System.out.println("sentenceCounter = " + sentenceCounter);
        Files.createDirectories(outRoot);
        roots.saveSortedByCounts(outRoot.resolve("roots.freq.txt"), " ");
        roots.saveSortedByKeys(outRoot.resolve("roots.keys.txt"), " ", Turkish.STRING_COMPARATOR_ASC);
        words.saveSortedByCounts(outRoot.resolve("words.freq.txt"), " ");
        words.saveSortedByKeys(outRoot.resolve("words.keys.txt"), " ", Turkish.STRING_COMPARATOR_ASC);
        words.removeSmaller(10);
        words.saveSortedByCounts(outRoot.resolve("words10.freq.txt"), " ");
        words.saveSortedByKeys(outRoot.resolve("words10.keys.txt"), " ", Turkish.STRING_COMPARATOR_ASC);
    }

    public static void biligCorpus() throws IOException {
        Path outRoot = root.resolve("bilig");

        System.out.println("Generate chunks.");
        List<String> chunks = loadChunks(root.resolve("bilig-corpus.txt"));
        Files.write(outRoot.resolve("chunks.txt"), chunks, StandardCharsets.UTF_8);

        System.out.println("Cleaning..");
        List<String> onlyTurkish = removeNonTurkish(outRoot.resolve("chunks.txt"));
        Files.write(outRoot.resolve("chunks-tr.txt"), onlyTurkish, StandardCharsets.UTF_8);

        System.out.println("Deacify..");
        List<String> deasc = deascify(outRoot.resolve("chunks-tr.txt"));
        Files.write(outRoot.resolve("chunks-tr-deasc.txt"), deasc, StandardCharsets.UTF_8);

        System.out.println("Find roots..");
        Path input = outRoot.resolve("chunks-tr-deasc.txt");
        List<String> paragraphs = Files.readAllLines(input);
        stats(paragraphs);
        generateHistograms(paragraphs, outRoot);
    }

    private static void newsCorpus() throws IOException {
        Path outRoot = root.resolve("haber");
        Path input = root.resolve("haber.corpus");
        List<String> paragraphs = getParagraphsFromCorpus(input);
        stats(paragraphs);
        generateHistograms(
                paragraphs,
                outRoot);
    }

    private static void subtitleCorpus() throws IOException {
        Path outRoot = root.resolve("subtitle");
        Path input = root.resolve("subtitle.corpus");
        List<String> paragraphs = Files.readAllLines(input);
        stats(paragraphs);
        generateHistograms(
                paragraphs,
                outRoot);
    }

    private static Path root = Paths.get("/media/data/corpora/word-freq");


    public static void main(String[] args) throws IOException {
        //biligCorpus();
        //newsCorpus();
        subtitleCorpus();
    }

}
