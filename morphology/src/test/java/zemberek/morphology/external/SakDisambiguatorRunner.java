package zemberek.morphology.external;

import java.io.File;
import java.io.IOException;

public class SakDisambiguatorRunner {
    File runnerRoot;
    File model;
    ProcessRunner processRunner;

    public SakDisambiguatorRunner(File runnerRoot, File model) {
        this.runnerRoot = runnerRoot;
        this.model = model;
        processRunner = new ProcessRunner(runnerRoot);
    }

    public void disambiguate(File ambigiousFile, File out) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "perl", "md.pl",
                "-disamb",
                model.getAbsolutePath(),
                ambigiousFile.getAbsolutePath(),
                out.getAbsolutePath());
        try {
            processRunner.execute(pb);
        } catch (InterruptedException e) {
            System.err.append("Operation interrupted unexpectedly.\n");
            e.printStackTrace();
        }
    }
}
