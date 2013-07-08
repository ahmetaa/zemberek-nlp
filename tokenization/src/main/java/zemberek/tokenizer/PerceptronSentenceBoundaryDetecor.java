package zemberek.tokenizer;

import zemberek.core.CountSet;
import zemberek.core.io.SimpleTextReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

//TODO: experimental work.
public class PerceptronSentenceBoundaryDetecor {


    String breakStr = ".!?";


    void train(File trainFile) throws IOException {
        List<String> sentences = SimpleTextReader.trimmingUTF8Reader(trainFile).asStringList();
        CountSet<String> features = new CountSet<>();
        for (int i = 0; i < sentences.size() - 1; i++) {
            String first = sentences.get(i);
            String next = sentences.get(i + 1);
            String all = first + " " + next;
            int boundary = first.length() - 1;

            int cursor=0;
            while(cursor<first.length()) {
                char c = first.charAt(cursor);
                if(breakStr.indexOf(c)<0)
                    continue;


            }
        }
    }


    void extractFeatures(String input, int pointer, CountSet<String> features) {

        // 1 letter before and after
        String firstLetter;
        if (pointer > 0)
            firstLetter = String.valueOf(input.charAt(pointer - 1));
        else
            firstLetter = "<s>";
        String secondLetter;
        if (pointer < input.length() - 1)
            secondLetter = String.valueOf(input.charAt(pointer + 1));
        else
            secondLetter = "</s>";

        features.add("1:" + firstLetter + secondLetter);
    }


}
