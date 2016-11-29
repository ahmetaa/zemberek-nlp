package zemberek.dependency;

import zemberek.core.process.ProcessRunner;

import java.io.File;
import java.io.IOException;

public class MaltJarParserRunner {
    File parserRoot;
    String modelName;
    ProcessRunner processRunner;
    File featureFile;
    int heapInMbytes;

    public MaltJarParserRunner(File parserRoot, File model, File features, int heapInMbytes) {
        this.parserRoot = parserRoot;
        this.modelName = model.getName().replaceAll("\\.mco", "");
        processRunner = new ProcessRunner(parserRoot);
        this.featureFile = features;
        this.heapInMbytes = heapInMbytes;
    }

    public MaltJarParserRunner(File parserRoot, String model, File features, int heapInMbytes) {
        this.parserRoot = parserRoot;
        this.modelName = model.replaceAll("\\.mco", "");
        processRunner = new ProcessRunner(parserRoot);
        this.featureFile = features;
        this.heapInMbytes = heapInMbytes;
    }

    public void parse(File input, File output) throws IOException {
        System.out.println("Parsing file:" + input);
        // java -jar malt.jar -c test -i examples/data/talbanken05_test.conll -o out.conll -m parse
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", "-Xmx" + heapInMbytes + "M", "-server", "malt.jar",
                "-c", modelName,
                "-i", input.getAbsolutePath(),
                "-o", output.getAbsolutePath(),
                "-F", featureFile.getAbsolutePath(),
                "-a", "nivrestandard",
                "-lso", "-s_0_-t_1_-d_2_-g_0.12_-c_0.7_-r_0.6_-e_.01",
                "-m", "parse");
        try {
            processRunner.execute(pb);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void train(File input) throws IOException {
        System.out.println("Training file:" + input);
        long start = System.currentTimeMillis();
        // java -jar malt.jar -c test -i examples/data/talbanken05_test.conll -o out.conll -m parse
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", "-Xmx" + heapInMbytes + "M", "-server", "malt.jar",
                "-c", modelName,
                "-i", input.getAbsolutePath(),
                "-F", featureFile.getAbsolutePath(),
                "-a", "nivrestandard",
                "-lso", "-s_0_-t_1_-d_2_-g_0.12_-c_0.7_-r_0.6_-e_.01",
                "-m", "learn");
        try {
            processRunner.execute(pb);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        MaltJarParserRunner maltRunner = new MaltJarParserRunner(
                new File("tools/malt-parser"),
                new File("model-base.mco"),
                new File("tools/malt-parser/features.xml"),
                512);
        //maltRunner.train(new File("tools/treebank-0.conll"));
        maltRunner.parse(new File("data/treebank/tr/turkish-metu-sabanci-test.conll"),
                new File("testout/parse-result.conll"));
    }


}
