package zemberek.langid;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import zemberek.langid.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class LanguageIdentifier {

    static Logger logger = Logger.getLogger(LanguageIdentifier.class.getName());

    public final int order;
    private Map<String, CharNgramLanguageModel> models = Maps.newHashMap();
    private String[] modelIdArray;

    public static final String UNKNOWN = "unk";

    private LanguageIdentifier(Map<String, CharNgramLanguageModel> models) {
        this.models = models;
        modelIdArray = new String[models.size()];
        int i = 0;
        for (String s : models.keySet()) {
            modelIdArray[i++] = s;
        }
        this.order = models.values().iterator().next().getOrder();
    }

    /**
     * Uses Internal count models.
     *
     * @param languages languages to identify
     * @return a LanguageIdentifier instance.
     * @throws java.io.IOException
     */
    public static LanguageIdentifier generateFromCounts(String[] languages) throws IOException {
        Map<String, CharNgramLanguageModel> modelMap = Maps.newHashMap();
        Set<String> availableLangIdSet = Sets.newHashSet(Language.languageIdSet());
        int order = 3;
        // generate models for required models on the fly.
        System.out.println("Generating models for:" + Arrays.toString(languages));
        for (String language : languages) {
            String l = language.toLowerCase();
            if (availableLangIdSet.contains(l)) {
                CharNgramCountModel countModel = CharNgramCountModel.load(Resources.getResource("models/langid/count/" + l + ".count").openStream());
                order = countModel.order;
                MapBasedCharNgramLanguageModel lm = MapBasedCharNgramLanguageModel.train(countModel);
                modelMap.put(l, lm);
                availableLangIdSet.remove(l);
            } else {
                logger.warning("Cannot find count model file for language: " + language);
            }
        }
        // generate garbage model from the remaining files if any left.
        if (!availableLangIdSet.isEmpty()) {
            System.out.println("Generating garbage model from remaining count models.");
            CharNgramCountModel garbageModel = new CharNgramCountModel("unk", order);
            for (String id : availableLangIdSet) {
                garbageModel.merge(CharNgramCountModel.load(Resources.getResource("models/langid/count/" + id + ".count").openStream()));
            }
            MapBasedCharNgramLanguageModel lm = MapBasedCharNgramLanguageModel.train(garbageModel);
            modelMap.put(lm.getId(), lm);
        }
        return new LanguageIdentifier(modelMap);
    }

    public static LanguageIdentifier generateFromCounts(File countModelsDir, String[] languages) throws IOException {
        Map<String, File> modelFileMap = Maps.newHashMap();
        Map<String, CharNgramLanguageModel> modelMap = Maps.newHashMap();
        File[] allFiles = countModelsDir.listFiles();
        int order = 3;
        if (allFiles == null || allFiles.length == 0)
            throw new IllegalArgumentException("There is no file in:" + countModelsDir);
        for (File file : allFiles) {
            final String langStr = file.getName().substring(0, file.getName().indexOf("."));
            modelFileMap.put(langStr, file);
        }
        // generate models for required models on the fly.
        System.out.println("Generating models for:" + Arrays.toString(languages));

        for (String language : languages) {
            String l = language.toLowerCase();
            if (modelFileMap.containsKey(l)) {
                CharNgramCountModel countModel = CharNgramCountModel.load(modelFileMap.get(l));
                order = countModel.order;
                MapBasedCharNgramLanguageModel lm = MapBasedCharNgramLanguageModel.train(countModel);
                modelMap.put(l, lm);
                modelFileMap.remove(l);
            } else {
                System.out.println("Cannot find count model file for language " + language);
            }
        }
        // generate garbage model from the remaining files if any left.
        if (!modelFileMap.isEmpty()) {
            System.out.println("Generating garbage model from remaining count models.");
            CharNgramCountModel garbageModel = new CharNgramCountModel("unk", order);
            for (File file : modelFileMap.values()) {
                garbageModel.merge(CharNgramCountModel.load(file));
            }
            MapBasedCharNgramLanguageModel lm = MapBasedCharNgramLanguageModel.train(garbageModel);
            modelMap.put(lm.getId(), lm);
        }
        return new LanguageIdentifier(modelMap);
    }

    public List<CharNgramLanguageModel> getModels() {
        return Lists.newArrayList(models.values());
    }

    private int[] getSequencial(String content) {
        int[] vals = new int[content.length() - order + 1];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = i;
        }
        return vals;
    }

    private int[] getStepping(String content, int gramAmount) {
        int gramIndexLimit = content.length() - order + 1;
        // if gram count value is larder than the limit value, we get the max amount
        int gramCount = gramAmount;
        if (gramCount > gramIndexLimit)
            gramCount = gramIndexLimit;
        int s = gramIndexLimit / gramCount;
        int step = s < 3 ? 3 : s; // by default we make a stepping of 3
        int[] vals = new int[gramCount];

        int samplingPoint = 0;
        int startPoint = 0;

        for (int i = 0; i < vals.length; i++) {
            vals[i] = samplingPoint;
            samplingPoint += step;
            if (samplingPoint >= gramIndexLimit) {
                startPoint++;
                samplingPoint = startPoint;
            }
        }
        return vals;
    }

    private String[] getGrams(String content, int[] gramStarts) {
        String[] grams = new String[gramStarts.length];
        int i = 0;
        for (int gramStart : gramStarts) {
            grams[i++] = content.substring(gramStart, gramStart + order);
        }
        return grams;
    }

    public Set<String> getLanguages() {
        Set<String> all = Sets.newHashSet(models.keySet());
        all.remove(BaseCharNgramModel.UNKNOWN);
        return all;
    }


    private static Map<String, CharNgramLanguageModel> getModelsFromDir(File dir, boolean compressed) throws IOException {
        Map<String, CharNgramLanguageModel> map = Maps.newHashMap();
        if (!dir.exists())
            throw new IllegalArgumentException("Training data directory does not exist:" + dir);
        if (!dir.isDirectory())
            throw new IllegalArgumentException(dir + "is not a directory");
        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0)
            throw new IllegalArgumentException("There is no file in:" + dir);
        for (File file : allFiles) {
            final String langStr = file.getName().substring(0, file.getName().indexOf("."));
            if (compressed) {
                map.put(langStr, CompressedCharNgramModel.load(file));
            } else {
                map.put(langStr, MapBasedCharNgramLanguageModel.loadCustom(file));
            }
        }
        if (map.size() == 0) {
            throw new IllegalArgumentException("There is no model file in dir:" + dir);
        }
        return map;
    }

    public static LanguageIdentifier fromUncompressedModelsDir(File dir) throws IOException {
        return new LanguageIdentifier(getModelsFromDir(dir, false));
    }

    public static LanguageIdentifier fromCompressedModelsDir(File dir) throws IOException {
        return new LanguageIdentifier(getModelsFromDir(dir, true));
    }

    public boolean modelExists(String modelId) {
        return models.containsKey(modelId.toLowerCase());
    }

    private static class ModelScore implements Comparable<ModelScore> {
        CharNgramLanguageModel model;
        double score;

        private ModelScore(CharNgramLanguageModel model, double score) {
            this.model = model;
            this.score = score;
        }

        @Override
        public int compareTo(ModelScore modelScore) {
            return Double.compare(modelScore.score, score);
        }

        public String toString() {
            return model.getId() + " : " + score;
        }
    }

    public String identifyFull(String input) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        return identifyWithSampling(clean, clean.length());
    }

    public String identifyFull(String input, double confidenceThreshold) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        IdResult result = identifyConf(clean, getSequencial(clean));
        if (result.score >= confidenceThreshold)
            return result.id;
        else
            return UNKNOWN;
    }

    /**
     * This methods gets 50 samples from the input for detecting the language of the content.
     *
     * @param input content
     * @return identified language
     */
    public String identifyWithSampling(String input) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        return identify(clean, getStepping(clean, 50));
    }

    public String identifyWithSampling(String input, int maxSampleCount) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        return identify(clean, getStepping(clean, maxSampleCount));
    }

    public String identifyWithSampling(String input, int maxSampleCount, double threshold) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        IdResult result = identifyConf(clean, getStepping(clean, maxSampleCount));
        if (result.score >= threshold)
            return result.id;
        else
            return BaseCharNgramModel.UNKNOWN;
    }

    public String identifyWithSamplingLarge(String input, int maxSampleCount) {
        String clean = preprocess(input);
        if (input.length() < order)
            return UNKNOWN;
        return identifyWithElimination(clean, getStepping(clean, maxSampleCount));
    }

    private String identify(String input, int[] samplingPoints) {
        String[] grams = getGrams(input, samplingPoints);
        double max = -Double.MAX_VALUE;
        String maxLanguage = null;
        for (CharNgramLanguageModel model : models.values()) {
            double prob = 0;
            for (String gram : grams) {
                prob += model.gramProbability(gram);
            }
            if (prob > max) {
                max = prob;
                maxLanguage = model.getId();
            }
        }
        return maxLanguage;
    }

    private String identifyWithElimination(String input, int[] samplingPoints) {
        List<ModelScore> modelScores = Lists.newArrayListWithCapacity(modelIdArray.length);
        for (CharNgramLanguageModel model : models.values()) {
            modelScores.add(new ModelScore(model, 0));
        }
        String[] grams = getGrams(input, samplingPoints);
        int gramCounter = 0;
        int intervalCounter = 0;
        while (gramCounter < grams.length) {
            if (intervalCounter == 10 && modelScores.size() > 5) {
                intervalCounter = 0;
                Collections.sort(modelScores);
                modelScores = modelScores.subList(0, modelScores.size() / 2 + 1);
            }
            for (ModelScore modelScore : modelScores) {
                modelScore.score += modelScore.model.gramProbability(grams[gramCounter]);
            }
            intervalCounter++;
            gramCounter++;
        }
        Collections.sort(modelScores);
        return modelScores.get(0).model.getId();
    }

    private IdResult identifyConf(String input, int[] samplingPoints) {
        String[] grams = getGrams(input, samplingPoints);
        double[] scores = new double[models.size()];
        double max = -Double.MAX_VALUE;
        int i = 0;
        int best = 0;
        double totalScore = LogMath.LOG_ZERO;
        for (String modelId : modelIdArray) {
            CharNgramLanguageModel charNgramLanguageModel = models.get(modelId);
            double prob = 0;
            for (String gram : grams) {
                prob += charNgramLanguageModel.gramProbability(gram);
            }
            scores[i] = prob;
            totalScore = LogMath.logSum(totalScore, prob);
            if (prob > max) {
                max = prob;
                best = i;
            }
            i++;
        }
        return new IdResult(modelIdArray[best], Math.exp(scores[best] - totalScore));
    }

    static Pattern removeCharsPattern = Pattern.compile("[0-9\"#$%^&*\\(\\)_+\\-=/\\|\\\\<>\\{}\\[\\];:,]", Pattern.DOTALL | Pattern.MULTILINE);
    static Pattern whiteSpacePattern = Pattern.compile("\\s+", Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * Apply pre-processing by removing numbers, common punctuations and lowercasing the result.
     *
     * @param s input
     * @return preprocessed value.
     */
    public static String preprocess(String s) {
        // s = removeCharsPattern.matcher(s).replaceAll("");
        //  s = whiteSpacePattern.matcher(s).replaceAll(" ");
        return s.toLowerCase();
    }

    public static class IdResult {
        public final String id;
        public double score;

        public IdResult(String id, double score) {
            this.id = id;
            this.score = score;
        }
    }

    public static void main(String[] args) throws IOException {
        String[] languages = {"tr", "en", "ar"};
        LanguageIdentifier identifier = LanguageIdentifier.generateFromCounts(languages);
        String langId = identifier.identifyWithSampling("Ali okula gidecek");
        System.out.println("Dil: " + langId);

    }
}
