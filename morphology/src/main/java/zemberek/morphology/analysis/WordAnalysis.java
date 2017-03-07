package zemberek.morphology.analysis;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.lexicon.*;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.graph.SuffixSurfaceNode;
import zemberek.morphology.structure.StemAndEnding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class represents a single morphological parse of a word.
 * It contains the DictionaryItem of the word, the stem and a list of inflectional groups.
 * Every WordAnalysis must have at least one Inflectional group element in it .
 * First inflectional group element contains the primary and secondary Pos of the Dictionary item.
 * Last Inflectional group defines the pos of the word in general.
 */
public class WordAnalysis {

    public DictionaryItem dictionaryItem;
    public String root;
    public List<InflectionalGroup> inflectionalGroups;

    public static class SuffixData {
        public final Suffix suffix;
        public final String surface;
        public final String lex;

        public SuffixData(Suffix suffix, String surface, String lex) {
            this.suffix = suffix;
            this.surface = surface;
            this.lex = lex;
        }

        @Override
        public String toString() {
            if (surface.length() > 0)
                return suffix.id + ":" + surface;
            else return suffix.id;
        }
    }

    public WordAnalysis(DictionaryItem dictionaryItem, String root, List<InflectionalGroup> inflectionalGroups) {
        this.dictionaryItem = dictionaryItem;
        this.root = root;
        this.inflectionalGroups = inflectionalGroups;
    }

    /**
     * Returns the pronunciation of the parse. İt is usually the input of this parse.
     */
    public String getPronunciation() {
        StringBuilder sb = new StringBuilder();
        sb.append(dictionaryItem.pronunciation);
        for (InflectionalGroup inflectionalGroup : inflectionalGroups) {
            sb.append(inflectionalGroup.surfaceForm());
        }
        return sb.toString();
    }

    /**
     * Generates the input that produces this parse.
     */
    public String getSurfaceForm() {
        return root + getEnding();
    }

    public PrimaryPos getPos() {
        return inflectionalGroups.get(inflectionalGroups.size() - 1).pos;
    }

    public InflectionalGroup getLastIg() {
        return inflectionalGroups.get(inflectionalGroups.size() - 1);
    }

    public boolean isUnknown() {
        return dictionaryItem.isUnknown();
    }

    public boolean isRuntime() {
        return dictionaryItem.hasAttribute(RootAttribute.Runtime);
    }

    public DictionaryItem getDictionaryItem() {
        return dictionaryItem;
    }

    public static class InflectionalGroup {
        public List<SuffixData> suffixList = Lists.newArrayListWithCapacity(3);
        public PrimaryPos pos;
        public SecondaryPos spos;

        public static InflectionalGroup UNKNOWN = new InflectionalGroup(
                Lists.newArrayList(
                        new SuffixData(Suffix.UNKNOWN, "", "")), PrimaryPos.Unknown, SecondaryPos.Unknown);

        public InflectionalGroup(List<SuffixData> suffixList, PrimaryPos pos, SecondaryPos spos) {
            this.suffixList = suffixList;
            this.pos = pos;
            this.spos = spos;
        }

        public InflectionalGroup() {
        }

        public boolean containsSuffix(Suffix suffix) {
            for (SuffixData sd : suffixList) {
                if (sd.suffix.id.equals(suffix.id))
                    return true;
            }
            return false;
        }

        public String formatNoSurface() {
            StringBuilder sb = prefix();
            int j = 0;
            for (SuffixData suffixData : suffixList) {
                sb.append(suffixData.suffix.id);
                if (j < suffixList.size() - 1)
                    sb.append("+");
                j++;
            }
            sb.append(")");
            return sb.toString();
        }

        public String surfaceForm() {
            StringBuilder sb = new StringBuilder();
            for (SuffixData suffixData : suffixList) {
                sb.append(suffixData.surface);
            }
            return sb.toString();
        }

        public String formatNoEmpty() {
            StringBuilder sb = prefix();
            int j = 0;
            for (SuffixData suffixData : suffixList) {
                if (suffixData.surface.length() == 0) {
                    continue;
                }
                if (j > 0 && j < suffixList.size() - 1)
                    sb.append("+");
                sb.append(suffixData.suffix.id);
                j++;
            }
            if (sb.charAt(sb.length() - 1) == ';')
                sb.deleteCharAt(sb.length() - 1);
            sb.append(")");
            return sb.toString();
        }

        private StringBuilder prefix() {
            StringBuilder sb = new StringBuilder("(");
            sb.append(pos.shortForm);
            if (spos != null && spos != SecondaryPos.None)
                sb.append(",").append(spos.shortForm);
            if (suffixList.size() > 0)
                sb.append(";");
            return sb;
        }

        public String formatLong() {
            StringBuilder sb = prefix();
            sb.append(Joiner.on("+").join(suffixList));
            sb.append(")");
            return sb.toString();
        }

        @Override
        public String toString() {
            return formatLong();
        }
    }

    public List<String> suffixSurfaceList() {
        List<String> result = new ArrayList<>();
        for (InflectionalGroup ig : inflectionalGroups) {
            for (SuffixData sd : ig.suffixList) {
                result.add(sd.surface);
            }
        }
        return result;
    }

    /**
     * Splits the parse into stem and ending. Such as:
     * "kitaplar" -> "kitap-lar"
     * "kitabımdaki" -> "kitab-ımdaki"
     * "kitap" -> "kitap-"
     *
     * @return a StemAndEnding instance carrying stem and ending.
     * If ending has no surface content empty string is used.
     */
    public StemAndEnding getStemAndEnding() {
        return new StemAndEnding(root, getEnding());
    }

    /**
     * Returns the suffix letters. Such as:
     * "kitaplar" -> "lar"
     * "kitabımdaki" -> "ımdaki"
     * "kitap" -> ""
     *
     * @return letters in suffixes. Empty is there is no suffix surface form.
     */
    public String getEnding() {
        StringBuilder sb = new StringBuilder();
        for (InflectionalGroup ig : inflectionalGroups) {
            for (SuffixData data : ig.suffixList) {
                sb.append(data.surface);
            }
        }
        return sb.toString();
    }

    public String getRoot() {
        return root;
    }

    /**
     * Returns the dictionary form of the root.
     * Such as:
     * "gördüm"->"görmek"
     * "kitaplar"->"kitap"
     */
    public String getLemma() {
        return dictionaryItem.lemma;
    }

    /**
     * Returns surface forms list of all root and derivational roots of a parse.
     * Examples:
     * "kitaplar"  ->["kitap"]
     * "kitabım"   ->["kitab"]
     * "kitaplaşır"->["kitap", "kitaplaş"]
     * "kavrulduk" ->["kavr","kavrul"]
     */
    public List<String> getStems() {
        List<String> stems = Lists.newArrayListWithCapacity(2);
        stems.add(root);
        String previousStem = stems.get(0);
        if (inflectionalGroups.size() > 1) {
            previousStem = previousStem + inflectionalGroups.get(0).surfaceForm();
            for (int i = 1; i < inflectionalGroups.size(); i++) {
                InflectionalGroup ig = inflectionalGroups.get(i);
                SuffixData suffixData = ig.suffixList.get(0);
                if (suffixData.surface.length() > 0) {
                    String surface = suffixData.surface;
                    String stem = previousStem + surface;
                    if (!stems.contains(stem))
                        stems.add(stem);
                }
                previousStem = previousStem + ig.surfaceForm();
            }
        }
        return stems;
    }

    /**
     * Returns list of all lemmas of a parse.
     * Examples:
     * "kitaplar"  ->["kitap"]
     * "kitabım"   ->["kitap"]
     * "kitaplaşır"->["kitap", "kitaplaş"]
     * "kitaplaş"  ->["kitap", "kitaplaş"]
     * "arattıragörür" -> ["ara","arat","arattır","arattıragör"]
     */
    public List<String> getLemmas() {
        List<String> lemmas = Lists.newArrayListWithCapacity(2);
        lemmas.add(dictionaryItem.root);
        String previousStem = root;
        if (inflectionalGroups.size() > 1) {
            previousStem = previousStem + inflectionalGroups.get(0).surfaceForm();
            for (int i = 1; i < inflectionalGroups.size(); i++) {
                InflectionalGroup ig = inflectionalGroups.get(i);
                SuffixData suffixData = ig.suffixList.get(0);
                if (suffixData.surface.length() > 0) {
                    String surface = suffixData.surface;
                    if (suffixData.lex.endsWith("~k")) {
                        surface = surface.substring(0, surface.length() - 1) + "k";
                    }
                    String stem = previousStem + surface;
                    if (!lemmas.contains(stem))
                        lemmas.add(stem);
                }
                previousStem = previousStem + ig.surfaceForm();
            }
        }
        return lemmas;
    }

    public WordAnalysis(StemNode stemNode, List<SuffixSurfaceNode> suffixSurfaceNodes) {
        this.dictionaryItem = stemNode.getDictionaryItem();
        this.root = stemNode.surfaceForm;

        InflectionalGroup ig = new InflectionalGroup();
        List<InflectionalGroup> igs = Lists.newArrayListWithCapacity(2);
        ig.pos = dictionaryItem.primaryPos;
        ig.spos = dictionaryItem.secondaryPos;
        int j = 0;
        for (SuffixSurfaceNode suffixNode : suffixSurfaceNodes) {

            SuffixFormTemplate template = null;
            if (suffixNode.getSuffixForm() instanceof NullSuffixForm) {
                template = ((NullSuffixForm) suffixNode.getSuffixForm()).getTemplate();
            }

            // if node is a derivational node then we create a new ig and store the other
            if (template != null && (template instanceof DerivationalSuffixTemplate || j == 0)) {
                RootSuffix rootSuffix = (RootSuffix) template.getSuffix();
                if (ig.pos == null) {
                    ig.pos = rootSuffix.pos;
                }
                if (j > 0) {
                    igs.add(ig);
                    ig = new InflectionalGroup();
                    ig.pos = rootSuffix.pos;
                }

            } else {
                if (!(suffixNode.getSuffixForm().getSuffix() instanceof RootSuffix)) {
                    SuffixData suffixData = new SuffixData(
                            suffixNode.getSuffixForm().suffix,
                            suffixNode.surfaceForm,
                            suffixNode.getSuffixForm().generation);
                    ig.suffixList.add(suffixData);
                }
            }
            j++;
        }
        if (igs.isEmpty() || !ig.suffixList.isEmpty()) {
            igs.add(ig);
        }
        this.inflectionalGroups = igs;
    }

    public boolean containsSuffix(Suffix suffix) {
        for (InflectionalGroup inflectionalGroup : inflectionalGroups) {
            if (inflectionalGroup.containsSuffix(suffix))
                return true;
        }
        return false;
    }

    public List<Suffix> getSuffixes() {
        List<Suffix> suffixes = Lists.newArrayListWithCapacity(4);
        for (InflectionalGroup inflectionalGroup : inflectionalGroups) {
            for (SuffixData sd : inflectionalGroup.suffixList) {
                suffixes.add(sd.suffix);
            }
        }
        return suffixes;
    }

    public List<SuffixData> getSuffixDataList() {
        List<SuffixData> suffixes = Lists.newArrayListWithCapacity(4);
        for (InflectionalGroup inflectionalGroup : inflectionalGroups) {
            suffixes.addAll(inflectionalGroup.suffixList);
        }
        return suffixes;
    }

    private static Map<String, String> oflazerTable = Maps.newHashMap();

    static {
        oflazerTable.put("Inst", "Ins");
        oflazerTable.put("KeepDoing", "Repeat");
        oflazerTable.put("KeepDoing2", "Repeat");
        oflazerTable.put("WithoutDoing", "WithoutHavingDoneSo");
        oflazerTable.put("WithoutDoing2", "WithoutHavingDoneSo");
        oflazerTable.put("UnableToDo", "WithoutBeingAbleToHaveDoneSo");
        oflazerTable.put("UntilDoing", "Adamantly");
        oflazerTable.put("SinceDoing", "SinceDoingSo");
        oflazerTable.put("ByDoing", "ByDoingSo");
    }

    public String formatOflazer() {
        StringBuilder sb = new StringBuilder();
        sb.append(dictionaryItem.root).append("+");

        int i = 0;
        for (InflectionalGroup ig : inflectionalGroups) {
            if (i == 0) {
                if (ig.pos == PrimaryPos.Adverb) {
                    sb.append("Adverb"); // Oflazer uses Adverb, we use Adv
                } else
                    sb.append(ig.pos.shortForm);
                if (ig.spos != null && ig.spos != SecondaryPos.None)
                    sb.append("+").append(ig.spos.shortForm);
                if (!ig.suffixList.isEmpty())
                    sb.append("+");
            } else {
                if (sb.charAt(sb.length() - 1) == '+') {
                    sb.deleteCharAt(sb.length() - 1); // delete +
                }
                sb.append("^DB+").append(ig.pos.shortForm).append("+");
            }
            int j = 0;
            for (SuffixData sd : ig.suffixList) {
                String suffixId = sd.suffix.id;
                if (oflazerTable.containsKey(suffixId))
                    suffixId = oflazerTable.get(suffixId);
                sb.append(suffixId);
                if (j < ig.suffixList.size() - 1)
                    sb.append("+");
                j++;
            }
            i++;
        }
        return sb.toString();
    }

    public String formatLong() {
        StringBuilder sb = new StringBuilder("[(").append(dictionaryItem.lemma);
        sb.append(":").append(root).append(") ");

        for (InflectionalGroup ig : inflectionalGroups) {
            sb.append(ig.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    public String formatNoSurface() {
        StringBuilder sb = new StringBuilder("[(").append(dictionaryItem.lemma).append(") ");
        for (InflectionalGroup ig : inflectionalGroups) {
            sb.append(ig.formatNoSurface());
        }
        sb.append("]");
        return sb.toString();
    }

    public String formatNoEmpty() {
        StringBuilder sb = new StringBuilder("[(").append(dictionaryItem.lemma).append(") ");
        for (InflectionalGroup ig : inflectionalGroups) {
            sb.append(ig.formatNoEmpty());
        }
        sb.append("]");
        return sb.toString();
    }

    public String formatOnlyIgs() {
        StringBuilder sb = new StringBuilder();
        for (InflectionalGroup ig : inflectionalGroups) {
            sb.append(ig.formatNoSurface());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return formatLong();
    }
}




