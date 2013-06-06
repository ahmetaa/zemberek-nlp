package zemberek.langid.train;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import zemberek.langid.LanguageIdentifier;
import zemberek.langid.model.CharNgramCountModel;
import zemberek.langid.model.CompressedCharNgramModel;
import zemberek.langid.model.MapBasedCharNgramLanguageModel;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModelGenerator {

    public MapBasedCharNgramLanguageModel generateModel(ModelTrainData modelData) throws IOException {
        System.out.println("Training for:" + modelData.modelId + " Training files: " + modelData.modelFiles);
        CharNgramCountModel cm = getCountModel(modelData);
        return MapBasedCharNgramLanguageModel.train(cm);
    }

    public void compressModelToFile(MapBasedCharNgramLanguageModel model, File compressedFile) throws IOException {
        System.out.println("Compressing:" + model.id);
        CompressedCharNgramModel.compress(model, compressedFile);
    }

    /**
     * Defines the training data of a model. It has a unique id and can have multiple training files.
     */
    public static class ModelTrainData {
        int order;
        String modelId;
        List<File> modelFiles;
        int[] cutOffs;

        public ModelTrainData(int order, String modelId, List<File> modelFiles) {
            this.order = order;
            this.modelId = modelId;
            this.modelFiles = modelFiles;
        }

        public ModelTrainData(int order, String modelId, List<File> modelFiles, int[] cutOffs) {
            this.order = order;
            this.modelId = modelId;
            this.modelFiles = modelFiles;
            this.cutOffs = cutOffs;
        }

        public ModelTrainData(int order, String modelId, File... modelFiles) {
            this(order, modelId, Lists.newArrayList(modelFiles));
        }

        public void setCutOffs(int[] cutOffs) {
            this.cutOffs = cutOffs;
        }
    }


    Set<String> ignoreWords = Sets.newHashSet("://", "wikipedia", ".jpg", "image:", "file:", ".png");

    public CharNgramCountModel getCountModel(ModelTrainData modelTrainData) throws IOException {
        CharNgramCountModel countModel = new CharNgramCountModel(modelTrainData.modelId, modelTrainData.order);
        for (File file : modelTrainData.modelFiles) {
            System.out.println("Processing file:" + file);
            int ignoredCount = 0;
            Set<String> lines = new HashSet<>(com.google.common.io.Files.readLines(file, Charsets.UTF_8));
            for (String line : lines) {
                line = line.toLowerCase();
                boolean ignore = false;
                for (String ignoreWord : ignoreWords) {
                    if (line.contains(ignoreWord)) {
                        ignore = true;
                        ignoredCount++;
                        break;
                    }
                }
                if (!ignore) {
                    line = LanguageIdentifier.preprocess(line);
                    countModel.addGrams(line);
                }
            }
            System.out.println("Ignored lines for " + file + " : " + ignoredCount);
        }
        countModel.applyCutOffs(modelTrainData.cutOffs);
        countModel.dumpGrams(1);
        return countModel;
    }


    public void generateCountModelToDirectory(File outDir, List<ModelTrainData> modelTrainDataList) throws IOException {
        for (ModelTrainData modelTrainData : modelTrainDataList) {
            CharNgramCountModel countModel = getCountModel(modelTrainData);
            File modelFile = new File(outDir, modelTrainData.modelId + ".count");
            countModel.save(modelFile);
        }
    }
}
