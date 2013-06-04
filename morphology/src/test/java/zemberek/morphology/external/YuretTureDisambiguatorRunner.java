package zemberek.morphology.external;

import com.google.common.io.Resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class YuretTureDisambiguatorRunner {
    File runnerRoot;
    ProcessRunner processRunner;

    public YuretTureDisambiguatorRunner(File runnerRoot) {
        this.runnerRoot = runnerRoot;
        processRunner = new ProcessRunner(runnerRoot);
    }

    public void disambiguate(File ambigiousFile, File out) throws IOException {
        try {
            processRunner.pipe(null, new FileOutputStream(out),
                    new ProcessBuilder("cat", ambigiousFile.getAbsolutePath()),
                    new ProcessBuilder("perl", "singleline.pl"),
                    new ProcessBuilder("perl", "features.pl"),
                    new ProcessBuilder("perl", "ft-model-eval.pl")
            );
        } catch (InterruptedException e) {
            System.err.append("Operation interrupted unexpectedly.\n");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        YuretTureDisambiguatorRunner yr = new YuretTureDisambiguatorRunner(
                new File("/home/ahmetaa/apps/nlp-tools/Morphological-Disambiguator/Turkish-Yuret-Ture")
        );
        yr.disambiguate(new File(Resources.getResource("abc.txt").getFile()), new File(Resources.getResource("def.txt").getFile()));
    }
}
