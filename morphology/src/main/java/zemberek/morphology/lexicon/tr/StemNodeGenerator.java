package zemberek.morphology.lexicon.tr;

import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkicSeq;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.LexiconException;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.StemNode;
import zemberek.morphology.lexicon.graph.SuffixData;
import zemberek.morphology.lexicon.graph.TerminationType;

import java.util.EnumSet;

import static zemberek.core.turkish.RootAttribute.*;


/**
 * This class generates StemNode objects from Dictionary Items. Generated Nodes are not connected.
 */
public class StemNodeGenerator {

    TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;
    SuffixProvider suffixProvider;

    public StemNodeGenerator(SuffixProvider suffixProvider) {
        this.suffixProvider = suffixProvider;
    }

    EnumSet<RootAttribute> modifiers = EnumSet.of(
            Doubling,
            LastVowelDrop,
            ProgressiveVowelDrop,
            InverseHarmony,
            Voicing,
            Special,
            CompoundP3sg,
            CompoundP3sgRoot
    );

    /**
     * Generates StemNode objects from the dictionary item.
     * <p>Most of the time a single StemNode is generated.
     *
     * @param item DictionaryItem
     * @return one or more StemNode objects.
     */
    public StemNode[] generate(DictionaryItem item) {
        if (hasModifierAttribute(item)) {
            return generateModifiedRootNodes(item);
        } else {
            SuffixData[] roots = suffixProvider.defineSuccessorSuffixes(item);
            EnumSet<PhoneticAttribute> phoneticAttributes = calculateAttributes(item.pronunciation);
            StemNode stemNode = new StemNode(
                    item.root,
                    item,
                    TerminationType.TERMINAL,
                    phoneticAttributes,
                    EnumSet.noneOf(PhoneticExpectation.class));
            stemNode.exclusiveSuffixData = roots[0];
            return new StemNode[]{stemNode};
        }
    }


    public boolean hasModifierAttribute(DictionaryItem item) {
        for (RootAttribute attr : modifiers) {
            if (item.attributes.contains(attr))
                return true;
        }
        return false;
    }

    private EnumSet<PhoneticAttribute> calculateAttributes(String input) {
        return calculateAttributes(new TurkicSeq(input, alphabet));
    }

    private EnumSet<PhoneticAttribute> calculateAttributes(TurkicSeq sequence) {
        EnumSet<PhoneticAttribute> attrs = EnumSet.noneOf(PhoneticAttribute.class);
        // general phonetic attributes.
        if (sequence.vowelCount() > 0) {
            if (sequence.lastVowel().isRounded())
                attrs.add(PhoneticAttribute.LastVowelRounded);
            else
                attrs.add(PhoneticAttribute.LastVowelUnrounded);
            if (sequence.lastVowel().isFrontal()) {
                attrs.add(PhoneticAttribute.LastVowelFrontal);
            } else
                attrs.add(PhoneticAttribute.LastVowelBack);
        }
        if (sequence.lastLetter().isVowel()) {
            // elma
            attrs.add(PhoneticAttribute.LastLetterVowel);
        } else
            attrs.add(PhoneticAttribute.LastLetterConsonant);
        if (sequence.lastLetter().isVoiceless()) {
            attrs.add(PhoneticAttribute.LastLetterVoiceless);
            if (sequence.lastLetter().isStopConsonant()) {
                // kitap
                attrs.add(PhoneticAttribute.LastLetterVoicelessStop);
            }
        } else
            attrs.add(PhoneticAttribute.LastLetterNotVoiceless);
        return attrs;
    }

    private StemNode[] generateModifiedRootNodes(DictionaryItem dicItem) {

        if (dicItem.hasAttribute(RootAttribute.Special))
            return handleSpecialStems(dicItem);

        TurkicSeq modifiedSeq = new TurkicSeq(dicItem.pronunciation, alphabet);
        EnumSet<PhoneticAttribute> originalAttrs = calculateAttributes(dicItem.pronunciation);
        EnumSet<PhoneticAttribute> modifiedAttrs = originalAttrs.clone();
        EnumSet<PhoneticExpectation> originalExpectations = EnumSet.noneOf(PhoneticExpectation.class);
        EnumSet<PhoneticExpectation> modifiedExpectations = EnumSet.noneOf(PhoneticExpectation.class);

        for (RootAttribute attribute : dicItem.attributes) {

            // generate other boundary attributes and modified root state.
            switch (attribute) {
                case Voicing:
                    TurkicLetter last = modifiedSeq.lastLetter();
                    TurkicLetter modifiedLetter = alphabet.voice(last);
                    if (modifiedLetter == null) {
                        throw new LexiconException("Voicing letter is not proper in:" + dicItem);
                    }
                    if (dicItem.lemma.endsWith("nk"))
                        modifiedLetter = TurkishAlphabet.L_g;
                    modifiedSeq.changeLetter(modifiedSeq.length() - 1, modifiedLetter);
                    modifiedAttrs.remove(PhoneticAttribute.LastLetterVoicelessStop);
                    originalExpectations.add(PhoneticExpectation.ConsonantStart);
                    modifiedExpectations.add(PhoneticExpectation.VowelStart);
                    break;
                case Doubling:
                    modifiedSeq.append(modifiedSeq.lastLetter());
                    originalExpectations.add(PhoneticExpectation.ConsonantStart);
                    modifiedExpectations.add(PhoneticExpectation.VowelStart);
                    break;
                case LastVowelDrop:
                    if (modifiedSeq.lastLetter().isVowel()) {
                        modifiedSeq.delete(modifiedSeq.length() - 1);
                        modifiedExpectations.add(PhoneticExpectation.ConsonantStart);
                    } else {
                        modifiedSeq.delete(modifiedSeq.length() - 2);
                        if (!dicItem.primaryPos.equals(PrimaryPos.Verb)) {
                            originalExpectations.add(PhoneticExpectation.ConsonantStart);
                        }
                        modifiedExpectations.add(PhoneticExpectation.VowelStart);
                    }
                    break;
                case InverseHarmony:
                    originalAttrs.add(PhoneticAttribute.LastVowelFrontal);
                    originalAttrs.remove(PhoneticAttribute.LastVowelBack);
                    modifiedAttrs.add(PhoneticAttribute.LastVowelFrontal);
                    modifiedAttrs.remove(PhoneticAttribute.LastVowelBack);
                    break;
                case ProgressiveVowelDrop:
                    modifiedSeq.delete(modifiedSeq.length() - 1);
                    if (modifiedSeq.hasVowel()) {
                        modifiedAttrs = calculateAttributes(modifiedSeq);
                    }
                    break;
                default:
                    break;
            }
        }

        StemNode original = new StemNode(dicItem.root, dicItem, originalAttrs, originalExpectations);
        StemNode modified = new StemNode(modifiedSeq.toString(), dicItem, modifiedAttrs, modifiedExpectations);

        SuffixData[] roots = suffixProvider.defineSuccessorSuffixes(dicItem);

        original.exclusiveSuffixData = roots[0];
        modified.exclusiveSuffixData = roots[1];
        if (original.equals(modified))
            return new StemNode[]{original};

        modified.setTermination(TerminationType.NON_TERMINAL);
        if (dicItem.hasAttribute(RootAttribute.CompoundP3sgRoot))
            original.setTermination(TerminationType.NON_TERMINAL);
        return new StemNode[]{original, modified};
    }

    // handle special words such as demek-diyecek , beni-bana
    private StemNode[] handleSpecialStems(DictionaryItem item) {

        TurkishSuffixes turkishSuffixes = (TurkishSuffixes) suffixProvider;
        String id = item.getId();


        if (id.equals("yemek_Verb")) {
            StemNode[] stems;
            stems = new StemNode[3];
            stems[0] = new StemNode("ye", item, TerminationType.TERMINAL, calculateAttributes(item.root));
            stems[0].exclusiveSuffixData.add(turkishSuffixes.Verb_Ye.allConnections());
            EnumSet<PhoneticAttribute> attrs = calculateAttributes(item.root);
            attrs.remove(PhoneticAttribute.LastLetterVowel);
            attrs.add(PhoneticAttribute.LastLetterConsonant);
            stems[1] = new StemNode("y", item, TerminationType.NON_TERMINAL, attrs, EnumSet.noneOf(PhoneticExpectation.class));
            stems[1].exclusiveSuffixData.add(turkishSuffixes.Verb_De_Ye_Prog.allConnections());
            stems[2] = new StemNode("yi", item, TerminationType.NON_TERMINAL, calculateAttributes(item.root));
            stems[2].exclusiveSuffixData.add(turkishSuffixes.Verb_Yi.allConnections());
            return stems;
        } else if (id.equals("demek_Verb")) {
            StemNode[] stems;
            stems = new StemNode[3];
            stems[0] = new StemNode("de", item, TerminationType.TERMINAL, calculateAttributes(item.root));
            stems[0].exclusiveSuffixData.add(turkishSuffixes.Verb_De.allConnections());
            EnumSet<PhoneticAttribute> attrs = calculateAttributes(item.root);
            attrs.remove(PhoneticAttribute.LastLetterVowel);
            attrs.add(PhoneticAttribute.LastLetterConsonant);
            stems[1] = new StemNode("d", item, TerminationType.NON_TERMINAL, attrs, EnumSet.noneOf(PhoneticExpectation.class));
            stems[1].exclusiveSuffixData.add(turkishSuffixes.Verb_De_Ye_Prog.allConnections());
            stems[2] = new StemNode("di", item, TerminationType.NON_TERMINAL, calculateAttributes(item.root));
            stems[2].exclusiveSuffixData.add(turkishSuffixes.Verb_Di.allConnections());
            return stems;
        } else if (id.equals("ben_Pron_Pers") || id.equals("sen_Pron_Pers")) {
            StemNode[] stems;
            stems = new StemNode[2];
            if (item.lemma.equals("ben")) {
                stems[0] = new StemNode(item.root, item, TerminationType.TERMINAL, calculateAttributes(item.root));
                stems[0].exclusiveSuffixData.add(turkishSuffixes.PersPron_Ben.allConnections());
                stems[1] = new StemNode("ban", item, TerminationType.NON_TERMINAL, calculateAttributes("ban"));
            }
            else {
                stems[0] = new StemNode(item.root, item, TerminationType.TERMINAL, calculateAttributes(item.root));
                stems[0].exclusiveSuffixData.add(turkishSuffixes.PersPron_Sen.allConnections());
                stems[1] = new StemNode("san", item, TerminationType.NON_TERMINAL, calculateAttributes("san"));
            }
            stems[1].exclusiveSuffixData.add(turkishSuffixes.PersPron_BanSan);
            return stems;
        } else {

            throw new IllegalArgumentException("Lexicon Item with special stem change cannot be handled:" + item);
        }
    }
}
