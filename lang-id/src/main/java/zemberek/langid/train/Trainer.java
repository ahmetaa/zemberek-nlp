package zemberek.langid.train;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import zemberek.langid.Language;
import zemberek.langid.LanguageIdentifier;
import zemberek.langid.model.CharNgramCountModel;
import zemberek.langid.model.CharNgramLanguageModel;
import zemberek.langid.model.CompressedCharNgramModel;
import zemberek.langid.model.MapBasedCharNgramLanguageModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class Trainer {

    File trainingDataDirs[];
    File countModelDir;

    ModelGenerator modelGenerator = new ModelGenerator();

    Multimap<String, File> langFileMap = HashMultimap.create();

    int[] cutOffs;
    int order;

    public Trainer(File trainingDataDirs[], File countModelDir, int order, int[] cutOffs) {
        System.out.println("Order:" + order);
        this.order = order;
        this.cutOffs = cutOffs;
        this.trainingDataDirs = trainingDataDirs;
        for (File training : trainingDataDirs) {
            if (!training.exists())
                throw new IllegalArgumentException("Training data directory does not exist:" + training);
            if (!training.isDirectory())
                throw new IllegalArgumentException(training + "is not a directory");
        }
        this.countModelDir = countModelDir;
        mkDir(this.countModelDir);
        for (File trainingDataDir : trainingDataDirs) {
            File[] allFiles = trainingDataDir.listFiles();
            if (allFiles == null || allFiles.length == 0)
                throw new IllegalArgumentException("There is no file in training dir:" + trainingDataDir);
            for (File file : allFiles) {
                if (file.isFile() && file.getName().contains("train")) {
                    final String langStr = file.getName().substring(0, file.getName().indexOf("-"));
                    langFileMap.put(langStr.toLowerCase(), file);
                }
            }
        }
        if (langFileMap.size() == 0) {
            throw new IllegalArgumentException("There is no training files in training dirs");
        }
    }

    private void mkDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs())
                throw new RuntimeException("Cannot create dir:" + dir);
            else
                System.out.println(dir + " is created.");
        }
    }

    public List<File> getFilesForModel(List<String> languageIds) {
        List<File> filesInGroup = new ArrayList<File>();
        for (String languageId : languageIds) {
            String key = languageId.toLowerCase();
            if (langFileMap.containsKey(key))
                filesInGroup.addAll(langFileMap.get(languageId));
            else
                System.out.println("Language " + languageId + " Does not exist in training data ");
        }
        return filesInGroup;
    }

    public List<File> getFilesForModel(String... languageIds) {
        return getFilesForModel(Lists.newArrayList(languageIds));
    }

    public List<String> getGarbageModelFiles(List<String> excludedLangIds) {
        List<String> lowercaseIds = new ArrayList<String>();
        for (String modelId : excludedLangIds) {
            lowercaseIds.add(modelId.toLowerCase());
        }
        List<String> garbageIds = new ArrayList<String>();
        for (String id : langFileMap.keySet()) {
            if (!lowercaseIds.contains(id))
                garbageIds.add(id);
        }
        return garbageIds;
    }


    public void train(List<String> modelIds) throws IOException {
        if (modelIds.isEmpty())
            System.out.println("There are no id's provided for training.");
        System.out.println("Order:" + order);
        for (String modelId : modelIds) {
            modelId = modelId.toLowerCase();
            System.out.println("Model:" + modelId);
            List<File> filesForModel = getFilesForModel(modelId);
            System.out.println("Files for model:" + filesForModel);
            ModelGenerator.ModelTrainData td = new ModelGenerator.ModelTrainData(order, modelId, filesForModel, cutOffs);
            train(td);
        }
    }

    public void trainParallel(Iterable<String> modelIds) throws IOException {
        List<Future<String>> futures = Lists.newArrayList();
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (final String modelId : modelIds) {
            futures.add(service.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String id = modelId.toLowerCase();
                    System.out.println("Model:" + id);
                    List<File> filesForModel = getFilesForModel(id);
                    System.out.println("Files for model:" + filesForModel);
                    ModelGenerator.ModelTrainData td = new ModelGenerator.ModelTrainData(order, id, filesForModel, cutOffs);
                    train(td);
                    return id;
                }
            }));
        }
        service.shutdown();
        for (Future<String> future : futures) {
            try {
                String s = future.get();
                System.out.println("Done:" + s);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void train(ModelGenerator.ModelTrainData td) throws IOException {
        File countFile = new File(countModelDir, td.modelId + ".count");
        CharNgramCountModel cm = modelGenerator.getCountModel(td);
        cm.save(countFile);
    }


    private void trainWithGarbage(List<String> languages) throws IOException {
        List<File> garbageFiles = getFilesForModel(getGarbageModelFiles(languages));
        ModelGenerator.ModelTrainData garbageData = new ModelGenerator.ModelTrainData(3, "unk", garbageFiles, cutOffs);
        train(languages);
        train(garbageData);
    }

    private void generateModelsToDir(File countDir, File modelDir, String[] languages, boolean compressed) throws IOException {
        LanguageIdentifier identifier = LanguageIdentifier.generateFromCounts(countDir, languages);
        List<CharNgramLanguageModel> models = identifier.getModels();
        mkDir(modelDir);
        for (CharNgramLanguageModel model : models) {
            System.out.println("Generating model for:" + model.getId());
            MapBasedCharNgramLanguageModel mbm = (MapBasedCharNgramLanguageModel) model;
            if (compressed) {
                File modelFile = new File(modelDir, model.getId() + ".clm");
                CompressedCharNgramModel.compress(mbm, modelFile);
            } else {
                File modelFile = new File(modelDir, model.getId() + ".lm");
                mbm.saveCustom(modelFile);
            }
        }
    }

    private static void train3gram() throws IOException {
        File[] trainingDirs = {
                new File("/home/kodlab/data/language-data/subtitle"),
                new File("/home/kodlab/data/language-data/wiki")
        };
        Set<String> large = Sets.newHashSet("JA", "KO", "ZH", "ML", "HI", "KM", "MY", "EL", "AR");
        Set<String> all = Sets.newHashSet(Arrays.asList(Language.allLanguages()));
        all.removeAll(large);

        File countModelDir = new File("/home/kodlab/data/language-data/models/counts3");
        Trainer trainer = new Trainer(
                trainingDirs,
                countModelDir,
                3,
                new int[]{50, 3, 3});

        trainer.trainParallel(all);

        Trainer trainer2 = new Trainer(
                trainingDirs,
                countModelDir,
                3,
                new int[]{50, 30, 30});
        trainer2.trainParallel(large);

        trainer = new Trainer(
                trainingDirs,
                countModelDir,
                3,
                new int[]{30, 2, 1});
        trainer.trainParallel(Lists.newArrayList("tr"));

        File compressedModelDir = new File("/home/kodlab/data/language-data/models/compressed3");
        String[] languages = {"tr", "en", "az", "ku", "ky", "de", "uz"};
        trainer.generateModelsToDir(countModelDir, compressedModelDir, languages, true);

        compressedModelDir = new File("/home/kodlab/data/language-data/models/compressedAll");
        trainer.generateModelsToDir(countModelDir, compressedModelDir, Language.allLanguages(), true);
    }

    private static void train2gram() throws IOException {
        File[] trainingDirs = {
                new File("/home/kodlab/data/language-data/subtitle"),
                new File("/home/kodlab/data/language-data/wiki")
        };
        Set<String> large = Sets.newHashSet("JA", "KO", "ZH", "ML", "HI", "KM", "MY", "EL", "AR");
        Set<String> all = Sets.newHashSet(Arrays.asList(Language.allLanguages()));
        all.removeAll(large);

        File countModelDir = new File("/home/kodlab/data/language-data/models/counts2");
        Trainer trainer = new Trainer(
                trainingDirs,
                countModelDir,
                2,
                new int[]{20, 2});

        trainer.trainParallel(all);

        Trainer trainer2 = new Trainer(
                trainingDirs,
                countModelDir,
                2,
                new int[]{50, 40});
        trainer2.trainParallel(large);

        File compressedModelDir = new File("/home/kodlab/data/language-data/models/compressed2");
        String[] languages = {"tr", "en"};
        trainer.generateModelsToDir(countModelDir, compressedModelDir, languages, true);
    }

    public static void trainSingle(String lang) throws IOException {
        File[] trainingDirs = {
                new File("/home/kodlab/data/language-data/subtitle"),
                new File("/home/kodlab/data/language-data/wiki")
        };
        File countModelDir = new File("/home/kodlab/data/language-data/models/counts3");
        Trainer trainer = new Trainer(
                trainingDirs,
                countModelDir,
                3,
                new int[]{50, 3, 3});
        trainer.train(Lists.newArrayList(lang));
    }

    public static void main(String[] args) throws IOException {
        //train3gram();
        trainSingle("ky");
    }
}
