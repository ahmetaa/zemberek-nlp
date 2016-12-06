package zemberek.lm.apps;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.kohsuke.args4j.Option;
import zemberek.core.CommandLineApplication;
import zemberek.core.logging.Log;
import zemberek.core.text.TextConverter;
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
public class CompressLm extends CommandLineApplication {

    @Option(name = "-in",
            required = true,
            usage = "Arpa input file.")
    public File arpaFile;

    @Option(name = "-out",
            required = true,
            usage = "SmoothLm output file.")
    public File compressedLmFile;

    @Option(name = "-tmpDir",
            usage = "Temporary folder for intermediate files. " +
                    "Operating System's temporary dir with a random folder is used by default.")
    public File tmpDir;

    @Option(name = "-spaceUsage",
            usage = "How many bits of space to be used for fingerprint, probability " +
                    "and back-off values in the compressed language model. Value must be in x-y-z format. " +
                    "By default it is 24-8-8 which means all values " +
                    "will be 2 bytes (16 bits). Values must be an order of 8, maximum 32 is allowed.")
    public String spaceUsageStr = "24-8-8";

    @Option(name = "-chunkBits",
            usage = "Defines the size of chunks when compressing very large models." +
                    " By default it is 21 bits meaning that chunks of 2^21 n-grams are used. Value must be between 16 to 31 (inclusive).")
    public int chunkBits = 21;

    @Option(name = "-fractionDigits",
            usage = "Probability value fractions are rounded to this digit count before applying quantization. Default " +
                    "value is 4 digits.")
    public int fractionDigits = 4;

    private int[] spaceUsage = new int[3];

    @Override
    public String getDescription() {
        return "This application generates a compressed binary language model (Smooth-Lm).";
    }

    public TextConverter textConverter;

    @Override
    protected void run() throws IOException {
        Preconditions.checkArgument(arpaFile.exists(), arpaFile + " does not exist. ");
        Log.info("Arpa file to convert:" + arpaFile);

        if (tmpDir == null) {
            tmpDir = com.google.common.io.Files.createTempDir();
            tmpDir.deleteOnExit();
            Log.info(("Using temporary directory: " + tmpDir));
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

        UncompressedToSmoothLmConverter converter = new UncompressedToSmoothLmConverter(compressedLmFile, tmpDir);
        converter.convertLarge(
                MultiFileUncompressedLm.generate(arpaFile, tmpDir, "utf-8", fractionDigits, textConverter).getLmDir(),
                new UncompressedToSmoothLmConverter.NgramDataBlock(spaceUsage[0], spaceUsage[1], spaceUsage[2]),
                chunkBits);
    }

    public static void main(String[] args) {
        new CompressLm().execute(args);
    }
}
