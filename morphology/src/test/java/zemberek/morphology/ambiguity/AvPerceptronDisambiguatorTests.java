package zemberek.morphology.ambiguity;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class AvPerceptronDisambiguatorTests {
    @Test
    @Ignore("Not a unit test")
    public void sakReference() throws IOException {
        File modelFile = new File("/home/kodlab/apps/nlp/sak/model-t.txt");
        File testFile = new File("/home/kodlab/apps/nlp/sak/test.z3.merge");
        AveragedPerceptronMorphDisambiguator.train(new File("/home/kodlab/apps/nlp/sak/train.z3.merge"), modelFile);
        AveragedPerceptronMorphDisambiguator disambiguator = new AveragedPerceptronMorphDisambiguator(modelFile);

        disambiguator.test(testFile);
    }
}
