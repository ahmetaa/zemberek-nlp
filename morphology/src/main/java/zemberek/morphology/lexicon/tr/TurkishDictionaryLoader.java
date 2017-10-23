package zemberek.morphology.lexicon.tr;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import zemberek.core.enums.StringEnum;
import zemberek.core.enums.StringEnumMap;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.core.turkish.*;
import zemberek.morphology.lexicon.*;
import zemberek.morphology.structure.Turkish;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static zemberek.core.turkish.TurkishAlphabet.L_l;
import static zemberek.core.turkish.TurkishAlphabet.L_r;

public class TurkishDictionaryLoader {

    private static final Splitter METADATA_SPLITTER = Splitter.on(";").trimResults().omitEmptyStrings();
    private static final Splitter POS_SPLITTER = Splitter.on(",").trimResults();
    private static final Splitter MORPHEMIC_ATTR_SPLITTER = Splitter.on(",").trimResults();

    public static final List<String> DEFAULT_DICTIONARY_RESOURCES = ImmutableList.of(
            "tr/master-dictionary.dict",
            "tr/secondary-dictionary.dict",
            "tr/non-tdk.dict",
            "tr/proper.dict",
            "tr/proper-from-corpus.dict",
            "tr/abbreviations.dict",
            "tr/locations-tr.dict"
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
            throw new LexiconException("Cannot parse lines [" + Arrays.toString(dictionaryLines) + "] with reason: "
                    + e.getMessage());
        }
    }

    public RootLexicon load(Iterable<String> dictionaryLines) {
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
        final List<File> DEFAULT_DICTIONARY_FILES = ImmutableList.of(
                new File(Resources.getResource("tr/master-dictionary.dict").getFile()),
                new File(Resources.getResource("tr/secondary-dictionary.dict").getFile()),
                new File(Resources.getResource("tr/non-tdk.dict").getFile()),
                new File(Resources.getResource("tr/proper.dict").getFile()),
                new File(Resources.getResource("tr/proper-from-corpus.dict").getFile()),
                new File(Resources.getResource("tr/abbreviations.dict").getFile())
        );
        List<String> lines = Lists.newArrayList();
        for (File file : DEFAULT_DICTIONARY_FILES) {
            lines.addAll(SimpleTextReader.trimmingUTF8Reader(file).asStringList());
        }
        return new TurkishDictionaryLoader(suffixProvider).load(lines);
    }

    enum MetaDataId implements StringEnum {
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

    // A simple class that holds raw word and metadata information. Represents a single line in dictionary.
    static class LineData {
        final String word;
        private final EnumMap<MetaDataId, String> metaData;

        LineData(String line) {
            this.word = Strings.subStringUntilFirst(line, " ");
            if (word.length() == 0)
                throw new LexiconException("Line " + line + " has no word data!");
            this.metaData = readMetadata(line);
        }

        String getMetaData(MetaDataId id) {
            return metaData==null ? null : metaData.get(id);
        }

        EnumMap<MetaDataId, String> readMetadata(String line) {
            String meta = line.substring(word.length()).trim();
            // No metadata defines, return.
            if (meta.isEmpty()) {
                return null;
            }
            // Check brackets.
            if (!meta.startsWith("[") || !meta.endsWith("]")) {
                throw new LexiconException("Malformed metadata, missing brackets. Should be: [metadata]. Line: " + line);
            }
            // Strip brackets.
            meta = meta.substring(1, meta.length() - 1);
            EnumMap<MetaDataId, String> metadataIds = Maps.newEnumMap(MetaDataId.class);
            for (String chunk : METADATA_SPLITTER.split(meta)) {
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
                metadataIds.put(id, chunkData);
            }
            return metadataIds;
        }

        boolean containsMetaData(MetaDataId metaDataId) {
            return metaData != null && metaData.containsKey(metaDataId);
        }
    }


    static final Pattern DASH_QUOTE_MATCHER = Pattern.compile("[\\-']");
    static class TextLexiconProcessor implements LineProcessor<RootLexicon> {

        RootLexicon rootLexicon = new RootLexicon();
        List<LineData> lateEntries = Lists.newArrayList();
        SuffixProvider suffixProvider;
        static final TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

        TextLexiconProcessor(SuffixProvider suffixProvider) {
            this.suffixProvider = suffixProvider;
        }

        public TextLexiconProcessor(RootLexicon lexicon, SuffixProvider suffixProvider) {
            rootLexicon = lexicon;
            this.suffixProvider = suffixProvider;
        }

        public boolean processLine(String line) throws IOException {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("##"))
                return true;
            try {
                LineData lineData = new LineData(line);
                // if a line contains references to other lines, we add them to lexicon later.
                if (!lineData.containsMetaData(MetaDataId.REF_ID) && !lineData.containsMetaData(MetaDataId.ROOTS))
                    rootLexicon.add(getItem(lineData));
                else
                    lateEntries.add(lineData);
            } catch (Exception e) {
                Log.info("Exception in line:" + line);
                throw new IOException(e);
            }
            return true;
        }

        public RootLexicon getResult() {
            for (LineData lateEntry : lateEntries) {
                if (lateEntry.containsMetaData(MetaDataId.REF_ID)) {
                    String referenceId = lateEntry.getMetaData(MetaDataId.REF_ID);
                    if (!referenceId.contains("_"))
                        referenceId = referenceId + "_Noun";
                    DictionaryItem refItem = rootLexicon.getItemById(referenceId);
                    if (refItem == null)
                        Log.warn("Cannot find reference item id " + referenceId);
                    DictionaryItem item = getItem(lateEntry);
                    item.referenceItem = refItem;
                    rootLexicon.add(item);
                }
                if (lateEntry.containsMetaData(MetaDataId.ROOTS)) { // this is a compound lemma with P3sg in it. Such as atkuyruğu
                    PosInfo posInfo = getPosData(lateEntry.getMetaData(MetaDataId.POS), lateEntry.word);
                    DictionaryItem item = rootLexicon.getItemById(lateEntry.word + "_" + posInfo.primaryPos.shortForm);
                    if (item == null) {
                        item = getItem(lateEntry); // we generate an item and add it.
                        rootLexicon.add(item);
                    }
                    String r = lateEntry.getMetaData(MetaDataId.ROOTS); // at-kuyruk
                    String root = r.replaceAll("-", ""); // atkuyruk

                    if (r.contains("-")) { // r = kuyruk
                        r = r.substring(r.indexOf('-') + 1);
                    }
                    List<DictionaryItem> refItems = rootLexicon.getMatchingItems(r); // check lexicon for [kuyruk]

                    EnumSet<RootAttribute> attrSet;
                    DictionaryItem refItem;
                    if (refItems.size() > 0) {
                        // use the item with lowest index value.
                        refItems.sort(Comparator.comparingInt(a -> a.index));
                        refItem = refItems.get(0); // grab the first Dictionary item matching to kuyruk. We will use it's attributes.
                        attrSet = refItem.attributes.clone();
                    } else {
                        attrSet = morphemicAttributes(null, root, posInfo);
                    }
                    attrSet.add(RootAttribute.CompoundP3sgRoot);
                    if (item.attributes.contains(RootAttribute.Ext))
                        attrSet.add(RootAttribute.Ext);

                    int index = 0;
                    if (rootLexicon.getItemById(root + "_" + item.primaryPos.shortForm) != null)
                        index = 1;
                    // generate a fake lemma for atkuyruk, use kuyruk's attributes.
                    DictionaryItem fakeRoot = new DictionaryItem(root, root, root, item.primaryPos, item.secondaryPos, attrSet, null, null, index);
                    fakeRoot.attributes.add(RootAttribute.Dummy);
                    fakeRoot.referenceItem = item;
                    rootLexicon.add(fakeRoot);
                }
            }
            return rootLexicon;
        }

        DictionaryItem getItem(LineData data) {
            PosInfo posInfo = getPosData(data.getMetaData(MetaDataId.POS), data.word);
            String cleanWord = generateRoot(data.word, posInfo);

            String indexStr = data.getMetaData(MetaDataId.INDEX);
            int index = 0;
            if (indexStr != null) {
                index = Integer.parseInt(indexStr);
            }

            ExclusiveSuffixData suffixData = getSuffixData(data.getMetaData(MetaDataId.SUFFIX));
            SuffixForm specialRoot = getSpecialRootSuffix(data.getMetaData(MetaDataId.ROOT_SUFFIX));

            String pronunciation = data.getMetaData(MetaDataId.PRONUNCIATION);
            if (pronunciation == null) {
                if (posInfo.primaryPos == PrimaryPos.Punctuation) {
                    //TODO: what to do with pronunciations of punctuations? For now we give them a generic one.
                    pronunciation = "a";
                } else if (new TurkicSeq(cleanWord, alphabet).hasVowel()) {
                    pronunciation = cleanWord;
                } else {
                    pronunciation = Turkish.inferPronunciation(cleanWord);
                }
            } else {
                pronunciation = pronunciation.toLowerCase(Turkish.LOCALE);
            }

            EnumSet<RootAttribute> attributes = morphemicAttributes(
                    data.getMetaData(MetaDataId.ATTRIBUTES),
                    pronunciation,
                    posInfo);

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
            if (posInfo.primaryPos == PrimaryPos.Punctuation) {
                return word;
            }
            // Strip -mek -mak from verbs.
            if (posInfo.primaryPos == PrimaryPos.Verb && isVerb(word)) {
                word = word.substring(0, word.length() - 3);
            }
            // Remove diacritics.
            word = word.toLowerCase(locale)
                    .replace('â', 'a')
                    .replace('î', 'i')
                    .replace('û', 'u');
            // Remove dashes
            return DASH_QUOTE_MATCHER.matcher(word).replaceAll("");
        }

        PosInfo getPosData(String posStr, String word) {
            if (posStr == null) {
                //infer the type.
                return new PosInfo(inferPrimaryPos(word), inferSecondaryPos(word));
            } else {
                PrimaryPos primaryPos = null;
                SecondaryPos secondaryPos = null;
                List<String> tokens = POS_SPLITTER.splitToList(posStr);

                if (tokens.size() > 2) {
                    throw new RuntimeException("Only two POS tokens are allowed in data chunk:" + posStr);
                }

                for (String token : tokens) {
                    if (!PrimaryPos.exists(token) && !SecondaryPos.exists(token)) {
                        throw new RuntimeException("Unrecognized pos data [" + token + "] in data chunk:" + posStr);
                    }
                }

                // Ques POS causes some trouble here. Because it is defined in both primary and secondary pos.
                for (String token : tokens) {

                    if (PrimaryPos.exists(token)) {
                        if (primaryPos == null) {
                            primaryPos = PrimaryPos.converter().getEnum(token);
                            continue;
                        } else if (!SecondaryPos.exists(token)) {
                            throw new RuntimeException("Multiple primary pos in data chunk:" + posStr);
                        }
                    }

                    if (SecondaryPos.exists(token)) {
                        if (secondaryPos == null) {
                            secondaryPos = SecondaryPos.converter().getEnum(token);
                        } else if (!PrimaryPos.exists(token)) {
                            throw new RuntimeException("Multiple secondary pos in data chunk:" + posStr);
                        }
                    }
                }
                // If there are no primary or secondary pos defined, try to infer them.
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
            return isVerb(word) ? PrimaryPos.Verb : PrimaryPos.Noun;
        }

        private boolean isVerb(String word) {
            return word.length() > 3 && (word.endsWith("mek") || word.endsWith("mak"));
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
                for (String s : MORPHEMIC_ATTR_SPLITTER.split(data)) {
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
                            && posData.secondaryPos != SecondaryPos.ProperNoun
                            && !attributes.contains(RootAttribute.NoVoicing)
                            && !attributes.contains(RootAttribute.InverseHarmony)) {
                        attributes.add(RootAttribute.Voicing);
                    }
                    if (word.endsWith("nk") || word.endsWith("og")) {
                        if (!attributes.contains(RootAttribute.NoVoicing)
                                && posData.secondaryPos != SecondaryPos.ProperNoun)
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

    public enum Digit {
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
        public SecondaryPos secondaryPos;

        Digit(String lemma, String patternStr, SecondaryPos secondaryPos) {
            this.lemma = lemma;
            this.pattern = Pattern.compile(patternStr);
            this.secondaryPos = secondaryPos;
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

