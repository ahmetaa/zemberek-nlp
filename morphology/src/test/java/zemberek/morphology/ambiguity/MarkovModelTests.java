package zemberek.morphology.ambiguity;

import org.junit.Ignore;
import org.junit.Test;
import zemberek.morphology.ambiguity.lm.LmGenerator;

import java.io.File;
import java.io.IOException;

public class MarkovModelTests {

    @Test
    @Ignore("Not a unit test")
    public void z3ModelA() throws IOException {
        //File workDir = new File("/home/kodlab/apps/nlp/sak");
        File workDir = new File("/home/afsina/apps/nlp/sak/disambiguator");
        File trainFile = new File(workDir, "train.z3.merge");
        File rootCorpus = new File(workDir, "root-corpus.z3.txt");
        File igCorpus = new File(workDir, "ig-corpus.z3.txt");
        Z3ModelA.generateTrainingCorpus(trainFile, rootCorpus, igCorpus);

        File rootArpaLm = new File(workDir, "root-lm.z3.arpa");
        File igArpaLm = new File(workDir, "ig-lm.z3.arpa");

        LmGenerator generator = new LmGenerator(new File("../bin/lm/lmplz").toPath(), 3);
        generator.generateArpaLm(rootCorpus, rootArpaLm);
        generator.generateArpaLm(igCorpus, igArpaLm);

        File rootSmoothLm = new File(workDir, "root-lm.z3.slm");
        File igSmoothLm = new File(workDir, "ig-lm.z3.slm");

        Z3ModelA.generateBinaryLm(rootArpaLm, rootSmoothLm);
        Z3ModelA.generateBinaryLm(igArpaLm, igSmoothLm);

        Z3ModelA disambiguator = new Z3ModelA(rootSmoothLm, igSmoothLm);

        File testFile = new File(workDir, "test.z3.merge");
        disambiguator.test(testFile);
    }

    @Test
    @Ignore("Not a unit test")
    public void z3MarkovModel() throws IOException {
        //File workDir = new File("/home/kodlab/apps/nlp/sak");
        File workDir = new File("/home/afsina/apps/nlp/sak/disambiguator");
        File trainFile = new File(workDir, "train.z3.merge");
        File rootCorpus = new File(workDir, "root-corpus.z3.txt");
        File igCorpus = new File(workDir, "ig-corpus.z3.txt");
        Z3MarkovModelDisambiguator.generateTrainingCorpus(trainFile, rootCorpus, igCorpus);

        File rootArpaLm = new File(workDir, "root-lm.z3.arpa");
        File igArpaLm = new File(workDir, "ig-lm.z3.arpa");

        LmGenerator generator = new LmGenerator(new File("../bin/lm/lmplz").toPath(), 3);
        generator.generateArpaLm(rootCorpus, rootArpaLm);
        generator.generateArpaLm(igCorpus, igArpaLm);

        File rootSmoothLm = new File(workDir, "root-lm.z3.slm");
        File igSmoothLm = new File(workDir, "ig-lm.z3.slm");

        Z3MarkovModelDisambiguator.generateBinaryLm(rootArpaLm, rootSmoothLm);
        Z3MarkovModelDisambiguator.generateBinaryLm(igArpaLm, igSmoothLm);

        Z3MarkovModelDisambiguator disambiguator = new Z3MarkovModelDisambiguator(rootSmoothLm, igSmoothLm);

        File testFile = new File(workDir, "test.z3.merge");
        disambiguator.test(testFile);
    }

    @Test
    @Ignore("Not a unit test")
    public void OflazerMarkovModel() throws IOException {
        File workDir = new File("/home/afsina/data/disamb");
        File trainFile = new File(workDir, "train.merge");
        File rootCorpus = new File(workDir, "root-corpus.txt");
        File igCorpus = new File(workDir, "ig-corpus.txt");
        //generateTrainingCorpus(trainFile, rootCorpus, igCorpus);

        File rootArpaLm = new File(workDir, "root-lm.arpa");
        File igArpaLm = new File(workDir, "ig-lm.arpa");

        //generateArpaLm(rootCorpus, rootArpaLm);
        //generateArpaLm(igCorpus, igArpaLm);

        File rootSmoothLm = new File(workDir, "root-lm.slm");
        File igSmoothLm = new File(workDir, "ig-lm.slm");

        //generateBinaryLm(rootArpaLm, rootSmoothLm );
        //generateBinaryLm(igArpaLm, igSmoothLm );

        MarkovModelDisambiguator disambiguator = new MarkovModelDisambiguator(rootSmoothLm, igSmoothLm);

        File testFile = new File(workDir, "test.merge");
        disambiguator.test(testFile);
    }

}
