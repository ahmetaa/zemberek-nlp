package zemberek.morphology.lexicon.tr;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import smoothnlp.core.io.SimpleTextReader;
import smoothnlp.core.io.Strings;
import zemberek.core.lexicon.PrimaryPos;
import zemberek.core.lexicon.SecondaryPos;
import zemberek.core.lexicon.tr.RootAttribute;
import zemberek.core.structure.*;
import zemberek.morphology.lexicon.*;

import zemberek.morphology.structure.Turkish;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static zemberek.core.structure.TurkishAlphabet.L_l;
import static zemberek.core.structure.TurkishAlphabet.L_r;

public class TurkishDictionaryLoader {

    public static final List<File> DEFAULT_DICTIONARY_FILES = ImmutableList.of(
            new File(Resources.getResource("tr/master-dictionary.dict").getFile()),
            new File(Resources.getResource("tr/secondary-dictionary.dict").getFile()),
            new File(Resources.getResource("tr/non-tdk.dict").getFile()),
            new File(Resources.getResource("tr/proper.dict").getFile())
    );

    public static final List<String> DEFAULT_DICTIONARY_RESOURCES = ImmutableList.of(
            "tr/master-dictionary.dict",
            "tr/secondary-dictionary.dict",
            "tr/non-tdk.dict",
            "tr/proper.dict"
    );

    SuffixProvider suffixProvider;

    public TurkishDictionaryLoader(SuffixProvider suffixProvider) {
        this.suffixProvider = suffixProvider;
    }

    public TurkishDictionaryLoader() {
        this.suffixProvider = new TurkishSuffixes();
    }

    public RootLexicon load(File input) throws IOException {
        return Files.readLines(input, Charsets.UTF_8, new TextLexiconProcessor(suffixProvider));
    }

    public RootLexicon loadInto(RootLexicon lexicon, File input) throws IOException {
        return Files.readLines(input, Charsets.UTF_8, new TextLexiconProcessor(lexicon, suffixProvider));
    }

    public DictionaryItem loadFromString(String dictionaryLine) {
        String lemma = dictionaryLine;
        if (dictionaryLine.contains(" ")) {
            lemma = dictionaryLine.substring(0, dictionaryLine.indexOf(" "));
        }
        return load(dictionaryLine).getMatchingItems(lemma).get(0);
    }

    public RootLexicon load(String... dictionaryLines) {
        TextLexiconProcessor processor = new TextLexiconProcessor(suffixProvider);
        try {
            for (String s : dictionaryLines) {
                processor.processLine(s);
            }
            return processor.getResult();
        } catch (Exception e) {
            throw new LexiconException("Cannot parse lines [" + Arrays.toString(dictionaryLines) + "] with reason: " + e.getMessage());
        }
    }

    public RootLexicon load(List<String> dictionaryLines) {
        TextLexiconProcessor processor = new TextLexiconProcessor(suffixProvider);
        for (String s : dictionaryLines) {
            try {
                processor.processLine(s);
            } catch (Exception e) {
                throw new LexiconException("Cannot load line '" + s + "' with reason: " + e.getMessage());
            }
        }
        return processor.getResult();
    }

    public static RootLexicon loadDefaultDictionaries(final SuffixProvider suffixProvider) throws IOException {
        List<String> lines = Lists.newArrayList();
        for (File file : DEFAULT_DICTIONARY_FILES) {
            lines.addAll(SimpleTextReader.trimmingUTF8Reader(file).asStringList());
        }
        return new TurkishDictionaryLoader(suffixProvider).load(lines);
    }

    static enum MetaDataId implements StringEnum {
        POS("P"),
        ATTRIBUTES("A"),
        REF_ID("Ref"),
        ROOTS("Roots"),
        ROOT_SUFFIX("RootSuffix"),
        PRONUNCIATION("Pr"),
        SUFFIX("S"),
        INDEX("Index");

        String form;
        static StringEnumMap<MetaDataId> toEnum = StringEnumMap.get(MetaDataId.class);

        MetaDataId(String form) {
            this.form = form;
        }

        @Override
        public String getStringForm() {
            return form;
        }
    }

    static class LineData {
        String word;
        EnumMap<MetaDataId, String> metaData = Maps.newEnumMap(MetaDataId.class);

        LineData(String word, EnumMap<MetaDataId, String> metaData) {
            this.word = word;
            this.metaData = metaData;
        }

        LineData(String word) {
            this.word = word;
        }

        boolean containsMetaData(MetaDataId metaDataId) {
            return metaData.containsKey(metaDataId);
        }
    }

    static class TextLexiconProcessor implements LineProcessor<RootLexicon> {

        RootLexicon rootLexicon = new RootLexicon();
        List<LineData> lateEntries = Lists.newArrayList();
        SuffixProvider suffixProvider;
        static final TurkishAlphabet alphabet = new TurkishAlphabet();

        TextLexiconProcessor(SuffixProvider suffixProvider) {
            this.suffixProvider = suffixProvider;
        }

        public TextLexiconProcessor(RootLexicon lexicon, SuffixProvider suffixProvider) {
            rootLexicon = lexicon;
            this.suffixProvider = suffixProvider;
        }

        /**
         * Extract tokens from the line. further processing is required as it only finds the raw word and mata data chunks.
         */
        LineData getTokens(String line) {
            EnumMap<MetaDataId, String> meta = Maps.newEnumMap(MetaDataId.class);

            String word = Strings.subStringUntilFirst(line, " ");
            if (word.length() == 0)
                throw new LexiconException("Line " + line + " has no word data!");

            String metaData = line.substring(word.length()).trim();
            if (metaData.length() == 0)
                return new LineData(word, meta);
            if (!metaData.startsWith("[") || !metaData.endsWith("]"))
                throw new LexiconException("Line " + line + " has malformed meta-data. It is missing start or end brackets.");

            metaData = metaData.substring(1, metaData.length() - 1);
            for (String chunk : Splitter.on(";").trimResults().omitEmptyStrings().split(metaData)) {
                if (!chunk.contains(":"))
                    throw new LexiconException("Line " + line + " has malformed meta-data chunk" + chunk + " it should have a ':' symbol.");
                String tokenIdStr = Strings.subStringUntilFirst(chunk, ":");
                if (!MetaDataId.toEnum.enumExists(tokenIdStr))
                    throw new LexiconException("Line " + line + " has malformed meta-data chunk" + chunk + " unknown chunk id:" + tokenIdStr);
                MetaDataId id = MetaDataId.toEnum.getEnum(tokenIdStr);
                String chunkData = Strings.subStringAfterFirst(chunk, ":");
                if (chunkData.length() == 0) {
                    throw new LexiconException("Line " + line + " has malformed meta-data chunk" + chunk + " no chunk data available");
                }
                meta.put(id, chunkData);
            }
            return new LineData(word, meta);
        }

        public boolean processLine(String line) throws IOException {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("##"))
                return true;
            try {
                LineData lineData = getTokens(line);
                // if a line contains references to other lines, we add them to lexicon later.
                if (!lineData.containsMetaData(MetaDataId.REF_ID) && !lineData.containsMetaData(MetaDataId.ROOTS))
                    rootLexicon.add(getItem(lineData));
                else
                    lateEntries.add(lineData);
            } catch (Exception e) {
                System.out.println("Exception in line:" + line);
                throw new IOException(e);
            }
            return true;
        }

        public RootLexicon getResult() {
            for (LineData lateEntry : lateEntries) {
                if (lateEntry.containsMetaData(MetaDataId.REF_ID)) {
                    String referenceId = lateEntry.metaData.get(MetaDataId.REF_ID);
                    if (!referenceId.contains("_"))
                        referenceId = referenceId + "_Noun";
                    DictionaryItem refItem = rootLexicon.getItemById(referenceId);
                    if (refItem == null)
                        System.out.println("Cannot find reference item id " + referenceId);
                    DictionaryItem item = getItem(lateEntry);
                    item.referenceItem = refItem;
                    rootLexicon.add(item);
                }
                if (lateEntry.containsMetaData(MetaDataId.ROOTS)) { // this is a compound lemma with P3sg in it. Such as atkuyruğu
                    PosInfo posInfo = getPosData(lateEntry.metaData.get(MetaDataId.POS), lateEntry.word);
                    DictionaryItem item = rootLexicon.getItemById(lateEntry.word + "_" + posInfo.primaryPos.shortForm);
                    if (item == null) {
                        item = getItem(lateEntry); // we generate an item and add it.
                        rootLexicon.add(item);
                    }
                    String r = lateEntry.metaData.get(MetaDataId.ROOTS); // at-kuyruk
                    String root = r.replaceAll("-", ""); // atkuyruk

                    if (r.contains("-")) { // r = kuyruk
                        r = r.substring(r.indexOf('-') + 1);
                    }
                    List<DictionaryItem> refItems = rootLexicon.getMatchingItems(r); // check lexicon for [kuyruk]

                    EnumSet<RootAttribute> attrSet;
                    DictionaryItem refItem;
                    if (refItems.size() > 0) {
                        refItem = refItems.get(0); // grab the first Dictionary item matching to kuyruk. We will use it's attributes.
                        attrSet = refItem.attrs.clone();
                    } else {
                        attrSet = morphemicAttributes(null, root, posInfo);
                    }
                    attrSet.add(RootAttribute.CompoundP3sgRoot);
                    if (item.attrs.contains(RootAttribute.Ext))
                        attrSet.add(RootAttribute.Ext);

                    int index = 0;
                    if (rootLexicon.getItemById(root + "_" + item.primaryPos.shortForm) != null)
                        index = 1;
                    // generate a fake lemma for atkuyruk, use kuyruk's attributes.
                    DictionaryItem fakeRoot = new DictionaryItem(root, root, root, item.primaryPos, item.secondaryPos, attrSet, null, null, index);
                    fakeRoot.dummy = true;
                    fakeRoot.referenceItem = item;
                    rootLexicon.add(fakeRoot);
                }
            }
            return rootLexicon;
        }

        DictionaryItem getItem(LineData data) {
            PosInfo posInfo = getPosData(data.metaData.get(MetaDataId.POS), data.word);
            String cleanWord = generateRoot(data.word, posInfo);

            String indexStr = data.metaData.get(MetaDataId.INDEX);
            int index = 0;
            if (indexStr != null)
                index = Integer.parseInt(indexStr);

            ExclusiveSuffixData suffixData = getSuffixData(data.metaData.get(MetaDataId.SUFFIX));
            SuffixForm specialRoot = getSpecialRootSuffix(data.metaData.get(MetaDataId.ROOT_SUFFIX));

            String pronunciation = data.metaData.get(MetaDataId.PRONUNCIATION);
            if (pronunciation == null) {
                if (new TurkicSeq(cleanWord, alphabet).hasVowel())
                    pronunciation = cleanWord;
                else
                    pronunciation = Turkish.inferPronunciation(cleanWord);
            }

            EnumSet<RootAttribute> attributes = morphemicAttributes(data.metaData.get(MetaDataId.ATTRIBUTES), pronunciation, posInfo);
            return new DictionaryItem(
                    data.word,
                    cleanWord,
                    pronunciation,
                    posInfo.primaryPos,
                    posInfo.secondaryPos,
                    attributes,
                    suffixData,
                    specialRoot,
                    index);
        }

        String generateRoot(String word, PosInfo posInfo) {
            if (posInfo.primaryPos == PrimaryPos.Punctuation)
                return word;
            if (posInfo.primaryPos == PrimaryPos.Verb)
                word = word.substring(0, word.length() - 3); // erase -mek -mak
            word = word.toLowerCase(locale).replaceAll("â", "a").replaceAll("î", "i").replaceAll("\u00e2", "u");
            return word.replaceAll("[\\-']", "");
        }

        PosInfo getPosData(String posStr, String word) {
            if (posStr == null) {
                //infer the type.
                return new PosInfo(inferPrimaryPos(word), inferSecondaryPos(word));
            } else {
                PrimaryPos primaryPos = null;
                SecondaryPos secondaryPos = null;
                for (String s : Splitter.on(",").trimResults().split(posStr)) {
                    if (PrimaryPos.converter().enumExists(s)) {
                        if (primaryPos != null && !SecondaryPos.converter().enumExists(s))
                            throw new RuntimeException("Multiple primary pos in data chunk:" + posStr);
                        else primaryPos = PrimaryPos.converter().getEnum(s);
                    } else if (SecondaryPos.converter().enumExists(s)) {
                        if (secondaryPos != null && !PrimaryPos.converter().enumExists(s))
                            throw new RuntimeException("Multiple secondary pos in data chunk:" + posStr);
                        else secondaryPos = SecondaryPos.converter().getEnum(s);
                    } else
                        throw new RuntimeException("Unrecognized pos data [" + s + "] in data chunk:" + posStr);
                }
                if (primaryPos == null) {
                    primaryPos = inferPrimaryPos(word);
                }
                if (secondaryPos == null) {
                    secondaryPos = inferSecondaryPos(word);
                }
                return new PosInfo(primaryPos, secondaryPos);
            }

        }

        private PrimaryPos inferPrimaryPos(String word) {
            if (Character.isUpperCase(word.charAt(0)))
                return PrimaryPos.Noun;
            else if (word.endsWith("mek") || word.endsWith("mak")) {
                return PrimaryPos.Verb;
            } else {
                return PrimaryPos.Noun;
            }
        }

        private SecondaryPos inferSecondaryPos(String word) {
            if (Character.isUpperCase(word.charAt(0))) {
                return SecondaryPos.ProperNoun;
            } else return SecondaryPos.None;
        }

        private EnumSet<RootAttribute> morphemicAttributes(String data, String word, PosInfo posData) {
            EnumSet<RootAttribute> attributesList = EnumSet.noneOf(RootAttribute.class);
            if (data == null) {
                //  if (!posData.primaryPos.equals(PrimaryPos.Punctuation))
                inferMorphemicAttributes(word, posData, attributesList);
            } else {
                for (String s : Splitter.on(",").split(data)) {
                    s = s.trim();
                    if (!RootAttribute.converter().enumExists(s))
                        throw new RuntimeException("Unrecognized attribute data [" + s + "] in data chunk :[" + data + "]");
                    RootAttribute rootAttribute = RootAttribute.converter().getEnum(s);
                    attributesList.add(rootAttribute);
                }
                inferMorphemicAttributes(word, posData, attributesList);
            }
            return EnumSet.copyOf(attributesList);
        }

        private SuffixForm getSpecialRootSuffix(String data) {
            if (data == null)
                return null;
            SuffixForm s = suffixProvider.getSuffixFormById(data);
            if (s == null)
                throw new LexiconException("Cannot identify special Form id:" + data + " in data chunk:[" + data + "]");
            else return suffixProvider.getSuffixFormById(data);
        }

        static Locale locale = new Locale("tr");

        private void inferMorphemicAttributes(
                String word,
                PosInfo posData,
                Set<RootAttribute> attributes) {
            TurkicSeq sequence = new TurkicSeq(word.toLowerCase(locale), alphabet);
            final TurkicLetter last = sequence.lastLetter();
            switch (posData.primaryPos) {
                case Verb:
                    // if a verb ends with a wovel, and -Iyor suffix is appended, last vowel drops.
                    if (last.isVowel()) {
                        attributes.add(RootAttribute.ProgressiveVowelDrop);
                        attributes.add(RootAttribute.Passive_In);
                    }
                    // if verb has more than 1 syllable and there is no Aorist_A label, add Aorist_I.
                    if (sequence.vowelCount() > 1 && !attributes.contains(RootAttribute.Aorist_A))
                        attributes.add(RootAttribute.Aorist_I);
                    // if verb has 1 syllable and there is no Aorist_I label, add Aorist_A
                    if (sequence.vowelCount() == 1 && !attributes.contains(RootAttribute.Aorist_I)) {
                        attributes.add(RootAttribute.Aorist_A);
                    }
                    if (last == L_l) {
                        attributes.add(RootAttribute.Passive_In);
                    }
                    if (last.isVowel() || (last == L_l || last == L_r) && sequence.vowelCount() > 1)
                        attributes.add(RootAttribute.Causative_t);
                    break;
                case Noun:
                case Adjective:
                case Duplicator:
                    // if a noun or adjective has more than one syllable and last letter is a stop consonant, add voicing.
                    if (sequence.vowelCount() > 1
                            && last.isStopConsonant()
                            && !attributes.contains(RootAttribute.NoVoicing)
                            && !attributes.contains(RootAttribute.InverseHarmony)) {
                        attributes.add(RootAttribute.Voicing);
                    }
                    if (word.endsWith("nk") || word.endsWith("og")) {
                        if (!attributes.contains(RootAttribute.NoVoicing))
                            attributes.add(RootAttribute.Voicing);
                    } else if (sequence.vowelCount() < 2 && !attributes.contains(RootAttribute.Voicing))
                        attributes.add(RootAttribute.NoVoicing);
                    break;
            }
        }

        private ExclusiveSuffixData getSuffixData(String data) {
            if (data == null)
                return null;
            ExclusiveSuffixData esd = new ExclusiveSuffixData();
            for (String token : Splitter.on(',').omitEmptyStrings().trimResults().split(data)) {
                if (token.length() < 2)
                    throw new LexiconException("Unexepected Suffix token in data chunk: " + data);
                String suffixId = token.substring(1);
                SuffixForm form = suffixProvider.getSuffixFormById(suffixId);
                if (form == null)
                    throw new LexiconException("Cannot identify Suffix Form id:" + suffixId + " in data chunk:" + data);

                switch (token.charAt(0)) {
                    case '+':
                        esd.accepts.add(form);
                        break;
                    case '-':
                        esd.rejects.add(form);
                        break;
                    case '*':
                        esd.onlyAccepts.add(form);
                        break;
                }
            }
            return esd;
        }

    }

    public static enum Digit {
        CARDINAL("#", "^[+\\-]?\\d+$", SecondaryPos.Cardinal),
        ORDINAL("#.", "^[+\\-]?[0-9]+[.]$", SecondaryPos.Ordinal),
        RANGE("#-#", "^[+\\-]?[0-9]+-[0-9]+$", SecondaryPos.Range),
        REAL("#,#", "^[+\\-]?[0-9]+[,][0-9]+$|^[+\\-]?[0-9]+[.][0-9]+$", SecondaryPos.Real),
        DISTRIB("#DIS", "^\\d+[^0-9]+$", SecondaryPos.Distribution),
        PERCENTAGE("%#", "^[%][0-9]+,[0-9]?+$|^[%][0-9]?+$|^[%][0-9].[0-9]?+$", SecondaryPos.Percentage),
        CLOCK("#:#", "^[0-9]{2}:[0-9]{2}$", SecondaryPos.Clock),
        DATE("##.##.####", "^[0-9]{2}\\.[0-9]{2}\\.[1-9]{4}$", SecondaryPos.Date);

        public String lemma;
        public Pattern pattern;
        public SecondaryPos spos;

        Digit(String lemma, String patternStr, SecondaryPos spos) {
            this.lemma = lemma;
            this.pattern = Pattern.compile(patternStr);
            this.spos = spos;
        }
    }

    static class PosInfo {
        PrimaryPos primaryPos;
        SecondaryPos secondaryPos;

        PosInfo(PrimaryPos primaryPos, SecondaryPos secondaryPos) {
            this.primaryPos = primaryPos;
            this.secondaryPos = secondaryPos;
        }

        @Override
        public String toString() {
            return primaryPos.shortForm + "-" + secondaryPos.shortForm;
        }
    }
}

