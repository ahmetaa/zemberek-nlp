package zemberek.langid;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.logging.Log;
import zemberek.core.math.LogMath;
import zemberek.langid.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class LanguageIdentifier {

    public static final int ELIMINATION_SAMPLE_STEP = 20;

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
     * Loads internal models from internal compressed resource folder.
     * Such as /models/langid has a folder named tr_group. It contains a group of language and unk compressed models.
     * for loading those modeles, fromInternalModelGroup("tr_group") should be called.
     *
     * @param groupId internal folder name
     * @return LanguageIdentifier
     * @throws IOException In case of an IO error.
     */
    public static LanguageIdentifier fromInternalModelGroup(String groupId) throws IOException {
        Set<String> languages = Sets.newHashSet();
        String languageList = "/models/" + groupId + "/langs.txt";
        try (InputStream is = Resources.getResource(LanguageIdentifier.class, languageList).openStream()) {
            String langLine = SimpleTextReader.trimmingReader(is, "utf-8").asString().trim();
            for (String langStr : Splitter.on(",").omitEmptyStrings().trimResults().split(langLine)) {
                languages.add(langStr);
            }
        }
        if (languages.size() == 0)
            throw new IllegalArgumentException("No language is provided!");
        Map<String, CharNgramLanguageModel> map = Maps.newHashMap();
        for (String language : languages) {
            String resourceName = "/models/" + groupId + "/" + language + ".clm";
            InputStream is = Resources.getResource(LanguageIdentifier.class, resourceName).openStream();
            if (is == null)
                throw new IllegalArgumentException("No internal model found: " + resourceName);
            CompressedCharNgramModel model = CompressedCharNgramModel.load(is);
            map.put(language, model);
        }
        return new LanguageIdentifier(map);
    }

    /**
     * Loads all internal models from internal compressed resource folder.
     *
     * @return LanguageIdentifier
     * @throws IOException In case of an IO error.
     */
    public static LanguageIdentifier fromInternalModels() throws IOException {
        String[] languages = Language.allLanguages();
        Map<String, CharNgramLanguageModel> map = Maps.newHashMap();
        Set<String> langs = Sets.newHashSet(languages);
        for (String language : langs) {
            String resourceName = "/models/compressed/" + language + ".clm";
            InputStream is = Resources.getResource(LanguageIdentifier.class, resourceName).openStream();
            if (is == null)
                throw new IllegalArgumentException("No internal model found: " + resourceName);
            CompressedCharNgramModel model = CompressedCharNgramModel.load(is);
            map.put(language, model);
        }
        return new LanguageIdentifier(map);
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
        Log.info("Generating models for:" + Arrays.toString(languages));

        for (String language : languages) {
            String l = language.toLowerCase();
            if (modelFileMap.containsKey(l)) {
                CharNgramCountModel countModel = CharNgramCountModel.load(modelFileMap.get(l));
                order = countModel.order;
                MapBasedCharNgramLanguageModel lm = MapBasedCharNgramLanguageModel.train(countModel);
                modelMap.put(l, lm);
                modelFileMap.remove(l);
            } else {
                Log.warn("Cannot find count model file for language " + language);
            }
        }
        // generate garbage model from the remaining files if any left.
        if (!modelFileMap.isEmpty()) {
            Log.info("Generating garbage model from remaining count models.");
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
        if (content.length() <= order)
            return new int[0];
        int[] vals = new int[content.length() - order + 1];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = i;
        }
        return vals;
    }

    private int[] getStepping(String content, int gramAmount) {
        if (content.length() <= order)
            return new int[0];
        if (gramAmount < 0)
            return getSequencial(content);
        int gramIndexLimit = content.length() - order + 1;
        // if gram count value is larger than the limit value, we get the max amount
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

    /**
     * @return The language id's that this identifier can detect.
     */
    public Set<String> getLanguages() {
        Set<String> all = Sets.newHashSet(models.keySet());
        all.remove(BaseCharNgramModel.UNKNOWN);
        return all;
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

    /**
     * Identifies input text's language.
     * It uses all data in the content and calculates score for all supplied languages.
     *
     * @param input input text
     * @return identified language's id
     */
    public String identify(String input) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        return identifySamples(clean, getStepping(clean, clean.length() - 1));
    }

    /**
     * Identifies input text's language using sampling. This methods gets maxSampleCount amount of samples
     * from the input for detecting the language of the content.
     *
     * @param input          content
     * @param maxSampleCount Max sampling value. Identifier gets this amount of samples from the content with stepping.
     *                       if content length is less than maxSampleCount, or maxSampleCount is -1 then sampling is not
     *                       applied and method behaves like {@link #identify(String) getComponentAt} method.
     * @return identified language's id
     */
    public String identify(String input, int maxSampleCount) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        return identifySamples(clean, getStepping(clean, maxSampleCount));
    }

    /**
     * When more than 5 languages are are tested for identification, this method applies elimination
     * of some models after every 20 scoring operation. This way method eliminates lower scored languages
     * from the operation.
     * after
     *
     * @param input          content
     * @param maxSampleCount Max sampling value. Identifier gets this amount of samples from the content with stepping.
     *                       if content length is less than maxSampleCount, or maxSampleCount is -1 then sampling is not
     *                       applied and method behaves like {@link #identify(String)} method.
     * @return identified language's id
     */
    public String identifyFast(String input, int maxSampleCount) {
        String clean = preprocess(input);
        if (input.length() < order)
            return UNKNOWN;
        return scoreWithElimination(clean, maxSampleCount).get(0).model.getId();
    }

    /**
     * @param input          input data
     * @param maxSampleCount Max sampling value. Identifier gets this amount of samples from the content with stepping.
     *                       if content length is less than maxSampleCount, or maxSampleCount is -1 then sampling is not
     *                       applied.
     * @return the identification results in a list for all languages and their respective scores. List is sorted by score
     *         in descending order. So best match is the first item.
     */
    public List<IdResult> getScores(String input, int maxSampleCount) {
        String clean = preprocess(input);
        if (input.length() < order)
            return Collections.emptyList();
        return convertModelScoresToIdscores(scoreFull(clean, maxSampleCount));
    }

    /**
     * This is similar to {@link #getScores(String, int)} method. But it eliminates low scored models during scoring.
     * Result length may be less than the possible language count. This method is substantially faster with possible
     * precision loss.
     *
     * @param input          input data
     * @param maxSampleCount Max sampling value. Identifier gets this amount of samples from the content with stepping.
     *                       if content length is less than maxSampleCount, or maxSampleCount is -1 then sampling is not
     *                       applied.
     * @return the identification results in a list for some languages and their respective scores. List is sorted by score
     *         in descending order. So best match is the first item.
     */
    public List<IdResult> getScoresFast(String input, int maxSampleCount) {
        String clean = preprocess(input);
        if (input.length() < order)
            return Collections.emptyList();
        return convertModelScoresToIdscores(scoreWithElimination(clean, maxSampleCount));
    }

    /**
     * Returns if any slice of the content contains the input language.
     *
     * @param content   content to check
     * @param language  two letter language id
     * @param sliceSize slice size (character length)
     * @return true if content contains the input language in any of the slices.
     */
    public boolean containsLanguage(String content, String language, int sliceSize) {
        return containsLanguage(content, language, sliceSize, -1);
    }

    /**
     * Returns if any slice of the content contains the input language.
     *
     * @param content   content to check
     * @param language  two letter language id
     * @param sliceSize slice size (character length)
     * @param samplePerSlice amount of samples to score in each slice.
     * @return true if content contains the input language in any of the slices.
     */
    public boolean containsLanguage(String content, String language, int sliceSize, int samplePerSlice) {
        if (sliceSize < 10) {
            throw new IllegalArgumentException("Slice size cannot be less than 10");
        }
        content = preprocess(content);
        if (sliceSize >= content.length())
            sliceSize = content.length();
        if (samplePerSlice < 0 || samplePerSlice > sliceSize)
            samplePerSlice = sliceSize;
        if (content.length() < order)
            return false;
        int sliceIndex = 0;
        int begin, end = 0;
        while (end < content.length()) {
            begin = sliceIndex * sliceSize;
            end = begin + sliceSize;
            if (content.length() - end < sliceSize)
                end = content.length();
            String slice = content.substring(begin, end);
            if (scoreWithElimination(slice, samplePerSlice).get(0).model.getId().equals(language))
                return true;
            sliceIndex++;
        }
        return false;
    }

    private String identifySamples(String input, int[] samplingPoints) {
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

    private List<ModelScore> scoreFull(String input, int maxSampleCount) {
        int[] samplingPoints;
        if (maxSampleCount == -1)
            samplingPoints = getStepping(input, input.length());
        else
            samplingPoints = getStepping(input, maxSampleCount);
        List<ModelScore> modelScores = Lists.newArrayListWithCapacity(modelIdArray.length);
        for (CharNgramLanguageModel model : models.values()) {
            modelScores.add(new ModelScore(model, 0));
        }
        String[] grams = getGrams(input, samplingPoints);
        int gramCounter = 0;
        while (gramCounter < grams.length) {
            for (ModelScore modelScore : modelScores) {
                modelScore.score += modelScore.model.gramProbability(grams[gramCounter]);
            }
            gramCounter++;
        }
        Collections.sort(modelScores);
        return modelScores;
    }

    private List<IdResult> convertModelScoresToIdscores(List<ModelScore> modelScores) {
        List<IdResult> res = new ArrayList<>(modelScores.size());
        for (int i = 0; i < modelScores.size(); i++) {
            ModelScore modelScore = modelScores.get(i);
            res.add(i, new IdResult(modelScore.model.getId(), modelScore.score));
        }
        return res;
    }

    private List<ModelScore> scoreWithElimination(String input, int maxSampleCount) {
        int[] samplingPoints;
        if (maxSampleCount == -1)
            samplingPoints = getStepping(input, input.length());
        else
            samplingPoints = getStepping(input, maxSampleCount);
        List<ModelScore> modelScores = Lists.newArrayListWithCapacity(modelIdArray.length);
        for (CharNgramLanguageModel model : models.values()) {
            modelScores.add(new ModelScore(model, 0));
        }
        String[] grams = getGrams(input, samplingPoints);
        int gramCounter = 0;
        int intervalCounter = 0;
        while (gramCounter < grams.length) {
            if (intervalCounter == ELIMINATION_SAMPLE_STEP && modelScores.size() > 2) {
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
        return modelScores;
    }

    // TODO make it public after proper testing
    private String identify(String input, int maxSampleCount, double threshold) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        IdResult result = identifyConf(clean, getStepping(clean, maxSampleCount));
        if (result.score >= threshold)
            return result.id;
        else
            return BaseCharNgramModel.UNKNOWN;
    }

    // TODO make it public after proper testing
    private String identify(String input, double confidenceThreshold) {
        String clean = preprocess(input);
        if (clean.length() < order)
            return UNKNOWN;
        IdResult result = identifyConf(clean, getSequencial(clean));
        if (result.score >= confidenceThreshold)
            return result.id;
        else
            return UNKNOWN;
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

    private static Pattern removeCharsPattern = Pattern.compile("[0-9\"#$%^&*()_+\\-=/|\\\\<>{}\\[\\];:,]", Pattern.DOTALL | Pattern.MULTILINE);
    private static Pattern whiteSpacePattern = Pattern.compile("\\s+", Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * Apply pre-processing by removing numbers, common punctuations and lowercasing the result.
     *
     * @param s input
     * @return preprocessed value.
     */
    public static String preprocess(String s) {
        s = removeCharsPattern.matcher(s).replaceAll("");
        s = whiteSpacePattern.matcher(s).replaceAll(" ");
        return s.toLowerCase();
    }

    public static class IdResult {
        public final String id;
        public double score;

        public IdResult(String id, double score) {
            this.id = id;
            this.score = score;
        }

        public String toString() {
            return id + " : " + String.format("%.3f", score);
        }
    }
}
