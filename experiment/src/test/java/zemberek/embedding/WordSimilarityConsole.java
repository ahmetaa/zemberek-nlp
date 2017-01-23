package zemberek.embedding;

import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class WordSimilarityConsole {

    public static void main(String[] args) throws Exception {
        Path vectorFile = Paths.get("/media/depo/data/aaa/corpora/model-large-min5-dim50.vec");
        Path binVectorFile = Paths.get("/media/depo/data/aaa/corpora/model-large-min5-dim50.vec.bin");
//        convertToBinary(vectorFile, binVectorFile);

        Path distanceListBin = Paths.get("/media/depo/data/aaa/corpora/distance-large-min5-dim50.bin");
        Path vocabFile = Paths.get("/media/depo/data/aaa/corpora/vocab-large-min5-dim50.bin");


/*
        WordDistances.saveDistanceListBin(
                binVectorFile,
                distanceListBin,
                vocabFile,
                10,
                50,
                24);
*/

        new WordSimilarityConsole().run(distanceListBin, vocabFile);
    }



    public void run(Path distanceListBinaryFile, Path vocabFile) throws IOException {

        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

        System.out.println("Loading from " + distanceListBinaryFile);
        DistanceList experiment = DistanceList.readFromBinary(distanceListBinaryFile, vocabFile);
        String input;
        System.out.println("Enter word:");
        Scanner sc = new Scanner(System.in);
        input = sc.nextLine();
        while (!input.equals("exit") && !input.equals("quit")) {
            if (!experiment.containsWord(input)) {
                Log.info(input + " cannot be found.");
                input = sc.nextLine();
                continue;
            }
            List<WordDistances.Distance> distances = experiment.getDistance(input);

            List<String> dist = new ArrayList<>(distances.size());
            dist.addAll(distances.stream().map(d -> d.word).collect(Collectors.toList()));
            System.out.println(String.join(" ", dist));

            List<String> noParse = new ArrayList<>();
            for (String s : dist) {
                List<WordAnalysis> tokens = morphology.analyze(s);
                if(tokens.size() == 0 || (tokens.size() == 1 && tokens.get(0).dictionaryItem.primaryPos == PrimaryPos.Unknown)) {
                    noParse.add(s);
                }
            }
            System.out.println(String.join(" ", noParse));
            input = sc.nextLine();
        }
    }


    private static void convertToBinary(Path vectorFile, Path binVectorFile) throws IOException {
        List<WordVector> vectors = WordVector.loadFromText(vectorFile);
        Log.info("Text vector file %s Loaded.", vectorFile);
        WordVector.writeAsBinary(vectors, binVectorFile);
        Log.info("Binary file %s saved.", binVectorFile);
    }
}
