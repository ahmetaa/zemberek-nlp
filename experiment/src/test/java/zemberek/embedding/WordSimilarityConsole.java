package zemberek.embedding;

import zemberek.core.logging.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WordSimilarityConsole {

    public static void main(String[] args) throws Exception {
        Path vectorFile = Paths.get("/media/depo/data/aaa/corpora/model-large-min10.vec");
        Path binVectorFile = Paths.get("/media/depo/data/aaa/corpora/model-large-min10.vec.bin");
//        convertToBinary(vectorFile, binVectorFile);
        Path distanceListBin = Paths.get("/media/depo/data/aaa/corpora/distance-large-min10.bin");
        Path vocabFile = Paths.get("/media/depo/data/aaa/corpora/vocab-large-min10.bin");

/*        DistanceList.saveDistanceListBin(
                binVectorFile,
                distanceListBin,
                vocabFile,
                200,
                50,
                24);*/

        new WordSimilarityConsole().run(distanceListBin, vocabFile);
    }

    public void run(Path distanceListBinaryFile, Path vocabFile) throws IOException {
        System.out.println("Loading from " + distanceListBinaryFile);
        List<DistanceList> list = DistanceList.readFromBinary(distanceListBinaryFile, vocabFile);
        Map<String, DistanceList> map = new HashMap<>(list.size());
        list.forEach(s -> map.put(s.source, s));
        String input;
        System.out.println("Enter word:");
        Scanner sc = new Scanner(System.in);
        input = sc.nextLine();
        while (!input.equals("exit") && !input.equals("quit")) {
            if (!map.containsKey(input)) {
                Log.info(input + " cannot be found.");
                input = sc.nextLine();
                continue;
            }
            DistanceList distances = map.get(input);

            List<String> dist = new ArrayList<>(distances.distances.length);
            for(DistanceList.Distance d : distances.distances) {
                dist.add(d.word);
            }
            System.out.println(String.join(" ", dist));

            input = sc.nextLine();
        }
    }


    private static void convertToBinary(Path vectorFile, Path binVectorFile) throws IOException {
        List<WordVector> vectors = WordVector.readFromText(vectorFile);
        Log.info("Text vector file %s Loaded.", vectorFile);
        WordVector.writeAsBinary(vectors, binVectorFile);
        Log.info("Binary file %s saved.", binVectorFile);
    }
}
