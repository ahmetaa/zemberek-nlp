package zemberek.morphology.ambiguity.lm;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import zemberek.core.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LmGenerator {

    int order;
    Path kenLmPath;

    public LmGenerator(Path kenLmPath, int order) {
        this.kenLmPath = kenLmPath;
        this.order = order;
    }

    public void generateArpaLm(File corpus, File arpaFile) throws IOException {

        File parent = arpaFile.getParentFile();
        Files.createDirectories(parent.toPath());
        File externalProcessLog = new File(parent, "generate-lm-process");

        List<String> params = Lists.newArrayList(
                kenLmPath.toString(),
                "--text", corpus.getAbsolutePath(),
                "--order", String.valueOf(order),
                "--arpa", arpaFile.getAbsolutePath(),
                "--sort_block", "256M",
                "--skip_symbols");

        ProcessBuilder pb = new ProcessBuilder(params);
        Log.info("Running :%s", Joiner.on(" ").join(pb.command()));
        if (!kenLmPath.toFile().setExecutable(true)) {
            throw  new IllegalStateException("Cannot execute from " + kenLmPath);
        }
        pb.redirectError(externalProcessLog);
        pb.redirectErrorStream(true);
        pb.redirectOutput(externalProcessLog);
        Process p = pb.start();
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
