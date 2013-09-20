package zemberek.lm.apps;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.kohsuke.args4j.Option;
import zemberek.core.CommandLineApplication;
import zemberek.core.logging.Log;
import zemberek.lm.compression.MultiFileUncompressedLm;
import zemberek.lm.compression.UncompressedToSmoothLmConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * A command line utility class for generating compressed SmoothLm model from an Arpa language model file.
 * Run the main method to see the options.
 */
public class ConvertToSmoothLm extends CommandLineApplication {

    @Option(name = "-arpaFile",
            required = true,
            usage = "Arpa input file.")
    public File arpaFile;

    @Option(name = "-smoothFile",
            required = true,
            usage = "SmoothLm output file.")
    public File smoothLmFile;

    @Option(name = "-tmpDir",
            usage = "Temporary folder for intermediate files. " +
                    "Operating System's temporary dir with a random folder is used by default.")
    public File tmpDir;

    @Option(name = "-spaceUsage",
            usage = "How many bits of space to be used for fingerprint, probability " +
                    "and back-off values in the compressed language model. Value must be in x-y-z format. " +
                    "By default it is 16-16-16 which means all values " +
                    "will be 2 bytes (16 bits). Values must be an order of 8, maximum 32 is allowed.")
    public String spaceUsageStr = "16-16-16";

    @Option(name = "-chunkBits",
            usage = "Defines the size of chunks when compressing very large models." +
                    " By default it is 21 bits meaning that chunks of 2^21 n-grams are used. Value must be between 16 to 31 (inclusive).")
    public int chunkBits = 21;

    private int[] spaceUsage = new int[3];

    @Override
    protected String getDescription() {
        return "This application generates a compressed binary language model (Smooth-Lm).";
    }

    @Override
    protected void run() throws IOException {
        Preconditions.checkArgument(arpaFile.exists(), arpaFile + " does not exist. ");
        Log.info("Arpa file to convert:" + arpaFile);

        if (tmpDir == null) {
            tmpDir = com.google.common.io.Files.createTempDir();
            tmpDir.deleteOnExit();
            Log.info(("Using temporar dir: " + tmpDir));
        } else {
            Files.createDirectories(tmpDir.toPath());
        }

        Preconditions.checkArgument(spaceUsageStr != null && spaceUsageStr.trim().length() > 0,
                "Improper -spaceUsageStr value: " + spaceUsageStr + ". Argument seems to be empty.");

        List<String> tokens = Lists.newArrayList(Splitter.on("-").omitEmptyStrings().trimResults().split(spaceUsageStr));

        Preconditions.checkArgument(tokens.size() == 3,
                "Improper -spaceUsageStr value: " + spaceUsageStr + ". Three value is expected in x-y-z format.");

        for (int i = 0; i < spaceUsage.length; i++) {
            try {
                final int val = Integer.parseInt(tokens.get(i));
                if (val <= 0 || val > 32 || val % 8 != 0) {
                    throw new IllegalArgumentException("Improper -spaceUsageStr value: " + spaceUsageStr + ". Values can be 8,16,24 or 32");
                }
                spaceUsage[i] = val;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Improper -spaceUsageStr value: " + spaceUsageStr + ". Values must be integers");
            }
        }
        Preconditions.checkArgument(chunkBits >= 16 && chunkBits <= 31,
                "Unexpected chunkBits value. Value must be between 16 to 31. But it is : " + chunkBits);

        UncompressedToSmoothLmConverter converter = new UncompressedToSmoothLmConverter(smoothLmFile, tmpDir);
        converter.convertLarge(
                MultiFileUncompressedLm.generate(arpaFile, tmpDir, "utf-8").getLmDir(),
                new UncompressedToSmoothLmConverter.NgramDataBlock(spaceUsage[0], spaceUsage[1], spaceUsage[2]),
                chunkBits);
    }

    public static void main(String[] args) {
        new ConvertToSmoothLm().execute(args);
    }
}
