package zemberek.morphology.analysis;

import static zemberek.core.turkish.RootAttribute.CompoundP3sg;
import static zemberek.core.turkish.RootAttribute.CompoundP3sgRoot;
import static zemberek.core.turkish.RootAttribute.Doubling;
import static zemberek.core.turkish.RootAttribute.InverseHarmony;
import static zemberek.core.turkish.RootAttribute.LastVowelDrop;
import static zemberek.core.turkish.RootAttribute.ProgressiveVowelDrop;
import static zemberek.core.turkish.RootAttribute.Voicing;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.morphotactics.AttributeSet;
import zemberek.morphology.morphotactics.MorphemeState;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.LexiconException;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * Hold stem->root-suffix transitions.
 * Such as
 * <pre>
 * elma->Noun
 * kitab->Noun
 * kitap->Noun
 * oku->Verb
 * </pre>
 */
public class StemTransitions {

  // TODO: this mechanism should be an abstraction that can also use a Trie
  private ArrayListMultimap<String, StemTransition> multiStems =
      ArrayListMultimap.create(1000, 2);
  private ArrayListMultimap<DictionaryItem, StemTransition> dictionaryMap =
      ArrayListMultimap.create(1000, 1);

  private Map<String, StemTransition> singleStems = Maps.newConcurrentMap();
  private Set<StemTransition> stemTransitions = Sets.newConcurrentHashSet();

  //TODO: check the lock mechanism
  private ReadWriteLock lock = new ReentrantReadWriteLock();

  RootLexicon lexicon;
  TurkishMorphotactics morphotactics;
  TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

  public StemTransitions(RootLexicon lexicon, TurkishMorphotactics morphotactics) {
    this.lexicon = lexicon;
    this.morphotactics = morphotactics;
    generateStemTransitions();
  }

  public Set<StemTransition> getTransitions() {
    return stemTransitions;
  }

  public RootLexicon getLexicon() {
    return lexicon;
  }

  private void generateStemTransitions() {
    for (DictionaryItem item : lexicon) {
      addDictionaryItem(item);
    }
  }

  private synchronized void addStemTransition(StemTransition stemTransition) {
    lock.writeLock().lock();
    try {
      final String surfaceForm = stemTransition.surface;
      if (multiStems.containsKey(surfaceForm)) {
        multiStems.put(surfaceForm, stemTransition);
      } else if (singleStems.containsKey(surfaceForm)) {
        multiStems.put(surfaceForm, singleStems.get(surfaceForm));
        singleStems.remove(surfaceForm);
        multiStems.put(surfaceForm, stemTransition);
      } else {
        singleStems.put(surfaceForm, stemTransition);
      }
      stemTransitions.add(stemTransition);
      if (!dictionaryMap.containsEntry(stemTransition.item, stemTransition)) {
        dictionaryMap.put(stemTransition.item, stemTransition);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private synchronized void removeStemNode(StemTransition stemTransition) {
    lock.writeLock().lock();
    try {
      final String surfaceForm = stemTransition.surface;
      if (multiStems.containsKey(surfaceForm)) {
        multiStems.remove(surfaceForm, stemTransition);
      } else if (singleStems.containsKey(surfaceForm)
          && singleStems.get(surfaceForm).item.equals(stemTransition.item)) {
        singleStems.remove(surfaceForm);
      }
      stemTransitions.remove(stemTransition);
      if (!dictionaryMap.containsEntry(stemTransition.item, stemTransition)) {
        dictionaryMap.remove(stemTransition.item, stemTransition);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public List<StemTransition> getMatchingStemTransitions(String stem) {
    if (singleStems.containsKey(stem)) {
      return Lists.newArrayList(singleStems.get(stem));
    } else if (multiStems.containsKey(stem)) {
      return Lists.newArrayList(multiStems.get(stem));
    } else {
      return Collections.emptyList();
    }
  }

  public List<StemTransition> getMatchingStemTransitions(DictionaryItem item) {
    if (dictionaryMap.containsKey(item)) {
      return dictionaryMap.get(item);
    } else {
      return Collections.emptyList();
    }
  }

  public void addDictionaryItem(DictionaryItem item) {
    try {
      List<StemTransition> transitions = generate(item);
      for (StemTransition transition : transitions) {
        addStemTransition(transition);
      }
    } catch (Exception e) {
      Log.warn("Cannot generate stem transition for %s with reason %s", item, e.getMessage());
    }
  }

  public void removeDictionaryItem(DictionaryItem item) {
    lock.writeLock().lock();
    try {
      List<StemTransition> transitions = generate(item);
      for (StemTransition transition : transitions) {
        removeStemNode(transition);
      }
    } catch (Exception e) {
      Log.warn("Cannot remove %s ", e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }


  EnumSet<RootAttribute> modifiers = EnumSet.of(
      Doubling,
      LastVowelDrop,
      ProgressiveVowelDrop,
      InverseHarmony,
      Voicing,
      CompoundP3sg,
      CompoundP3sgRoot
  );


  /**
   * Generates StemTransition objects from the dictionary item. <p>Most of the time a single
   * StemNode is generated.
   *
   * @param item DictionaryItem
   * @return one or more StemTransition objects.
   */
  public List<StemTransition> generate(DictionaryItem item) {
    if (specialRoots.contains(item.id)) {
      return handleSpecialRoots(item);
    }
    if (hasModifierAttribute(item)) {
      return generateModifiedRootNodes(item);
    } else {
      AttributeSet<PhoneticAttribute> phoneticAttributes = calculateAttributes(item.pronunciation);
      StemTransition transition = new StemTransition(
          item.root,
          item,
          phoneticAttributes,
          morphotactics.getRootState(item, phoneticAttributes)
      );

      return Lists.newArrayList(transition);
    }
  }

  private boolean hasModifierAttribute(DictionaryItem item) {
    for (RootAttribute attr : modifiers) {
      if (item.attributes.contains(attr)) {
        return true;
      }
    }
    return false;
  }

  private AttributeSet<PhoneticAttribute> calculateAttributes(CharSequence input) {
    return AttributesHelper.getMorphemicAttributes(input);
  }

  private List<StemTransition> generateModifiedRootNodes(DictionaryItem dicItem) {

    StringBuilder modifiedSeq = new StringBuilder(dicItem.pronunciation);

    AttributeSet<PhoneticAttribute> originalAttrs = calculateAttributes(dicItem.pronunciation);
    AttributeSet<PhoneticAttribute> modifiedAttrs = originalAttrs.copy();

    MorphemeState modifiedRootState = null;
    MorphemeState unmodifiedRootState = null;

    for (RootAttribute attribute : dicItem.attributes) {

      // generate other boundary attributes and modified root state.
      switch (attribute) {
        case Voicing:
          char last = alphabet.getLastChar(modifiedSeq);
          char voiced = alphabet.voice(last);
          if (last == voiced) {
            throw new LexiconException("Voicing letter is not proper in:" + dicItem);
          }
          if (dicItem.lemma.endsWith("nk")) {
            voiced = 'g';
          }
          modifiedSeq.setCharAt(modifiedSeq.length() - 1, voiced);
          modifiedAttrs.remove(PhoneticAttribute.LastLetterVoicelessStop);
          originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
          modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
          // TODO: find a better way for this.
          modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          break;
        case Doubling:
          modifiedSeq.append(alphabet.getLastChar(modifiedSeq));
          originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
          modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
          modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          break;
        case LastVowelDrop:
          TurkicLetter lastLetter = alphabet.getLastLetter(modifiedSeq);
          if (lastLetter.isVowel()) {
            modifiedSeq.deleteCharAt(modifiedSeq.length() - 1);
            modifiedAttrs.add(PhoneticAttribute.ExpectsConsonant);
            modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          } else {
            modifiedSeq.deleteCharAt(modifiedSeq.length() - 2);
            if (!dicItem.primaryPos.equals(PrimaryPos.Verb)) {
              originalAttrs.add(PhoneticAttribute.ExpectsConsonant);
            } else {
              unmodifiedRootState = morphotactics.verbLastVowelDropUnmodRoot_S;
              modifiedRootState = morphotactics.verbLastVowelDropModRoot_S;
            }
            modifiedAttrs.add(PhoneticAttribute.ExpectsVowel);
            modifiedAttrs.add(PhoneticAttribute.CannotTerminate);
          }
          break;
        case InverseHarmony:
          originalAttrs.add(PhoneticAttribute.LastVowelFrontal);
          originalAttrs.remove(PhoneticAttribute.LastVowelBack);
          modifiedAttrs.add(PhoneticAttribute.LastVowelFrontal);
          modifiedAttrs.remove(PhoneticAttribute.LastVowelBack);
          break;
        case ProgressiveVowelDrop:
          modifiedSeq.deleteCharAt(modifiedSeq.length() - 1);
          if (alphabet.containsVowel(modifiedSeq)) {
            modifiedAttrs = calculateAttributes(modifiedSeq);
          }
          modifiedAttrs.add(PhoneticAttribute.LastLetterDropped);
          break;
        default:
          break;
      }
    }

    if (unmodifiedRootState == null) {
      unmodifiedRootState = morphotactics.getRootState(dicItem, originalAttrs);
    }
    StemTransition original = new StemTransition(
        dicItem.root,
        dicItem,
        originalAttrs,
        unmodifiedRootState);

    // if modified root state is not defined in the switch block, get it from morphotactics.
    if (modifiedRootState == null) {
      modifiedRootState = morphotactics.getRootState(dicItem, modifiedAttrs);
    }

    StemTransition modified = new StemTransition(
        modifiedSeq.toString(),
        dicItem,
        modifiedAttrs,
        modifiedRootState);

    if (original.equals(modified)) {
      return Collections.singletonList(original);
    }
    return Lists.newArrayList(original, modified);
  }

  Set<String> specialRoots = Sets.newHashSet(
      "içeri_Noun", "içeri_Adj", "dışarı_Adj", "şura_Noun", "bura_Noun", "ora_Noun",
      "dışarı_Noun", "dışarı_Postp", "yukarı_Noun", "yukarı_Adj",
      "ben_Pron_Pers", "sen_Pron_Pers", "demek_Verb", "yemek_Verb",
      "birbiri_Pron_Quant", "çoğu_Pron_Quant", "öbürü_Pron_Quant", "birçoğu_Pron_Quant"
  );

  private List<StemTransition> handleSpecialRoots(DictionaryItem item) {

    String id = item.getId();
    AttributeSet<PhoneticAttribute> originalAttrs = calculateAttributes(item.pronunciation);
    StemTransition original, modified;
    MorphemeState unmodifiedRootState = morphotactics.getRootState(item, originalAttrs);

    switch (id) {
      case "içeri_Noun":
      case "içeri_Adj":
      case "dışarı_Adj":
      case "dışarı_Noun":
      case "dışarı_Postp":
      case "yukarı_Noun":
      case "yukarı_Adj":
      case "şura_Noun":
      case "bura_Noun":
      case "ora_Noun":
        original = new StemTransition(item.root, item, originalAttrs, unmodifiedRootState);

        MorphemeState rootForModified;
        switch (item.primaryPos) {
          case Noun:
            rootForModified = morphotactics.nounLastVowelDropRoot_S;
            break;
          case Adjective:
            rootForModified = morphotactics.adjLastVowelDropRoot_S;
            break;
          // TODO: check postpositive case. Maybe it is not required.
          case PostPositive:
            rootForModified = morphotactics.adjLastVowelDropRoot_S;
            break;
          default:
            throw new IllegalStateException("No root morpheme state found for " + item);
        }
        String m = item.root.substring(0, item.root.length() - 1);
        modified = new StemTransition(m, item, calculateAttributes(m), rootForModified);
        modified.getPhoneticAttributes().add(PhoneticAttribute.ExpectsConsonant);
        modified.getPhoneticAttributes().add(PhoneticAttribute.CannotTerminate);
        return Lists.newArrayList(original, modified);

      case "ben_Pron_Pers":
      case "sen_Pron_Pers":
        original = new StemTransition(item.root, item, originalAttrs, unmodifiedRootState);
        if (item.lemma.equals("ben")) {
          modified = new StemTransition("ban", item, calculateAttributes("ban"),
              morphotactics.pronPers_Mod_S);
        } else {
          modified = new StemTransition("san", item, calculateAttributes("san"),
              morphotactics.pronPers_Mod_S);
        }
        original.getPhoneticAttributes().add(PhoneticAttribute.UnModifiedPronoun);
        modified.getPhoneticAttributes().add(PhoneticAttribute.ModifiedPronoun);
        return Lists.newArrayList(original, modified);
      case "demek_Verb":
      case "yemek_Verb":
        original = new StemTransition(item.root, item, originalAttrs, morphotactics.vDeYeRoot_S);
        switch (item.lemma) {
          case "demek":
            modified = new StemTransition("di", item, calculateAttributes("di"),
                morphotactics.vDeYeRoot_S);
            break;
          default:
            modified = new StemTransition("yi", item, calculateAttributes("yi"),
                morphotactics.vDeYeRoot_S);
        }
        return Lists.newArrayList(original, modified);

      case "birbiri_Pron_Quant":
      case "çoğu_Pron_Quant":
      case "öbürü_Pron_Quant":
      case "birçoğu_Pron_Quant":
        original = new StemTransition(item.root, item, originalAttrs, morphotactics.pronQuant_S);

        switch (item.lemma) {
          case "birbiri":
            modified = new StemTransition("birbir", item, calculateAttributes("birbir"),
                morphotactics.pronQuantModified_S);
            break;
          case "çoğu":
            modified = new StemTransition("çok", item, calculateAttributes("çok"),
                morphotactics.pronQuantModified_S);
            break;
          case "öbürü":
            modified = new StemTransition("öbür", item, calculateAttributes("öbür"),
                morphotactics.pronQuantModified_S);
            break;
          default:
            modified = new StemTransition("birçok", item, calculateAttributes("birçok"),
                morphotactics.pronQuantModified_S);
            break;
        }
        original.getPhoneticAttributes().add(PhoneticAttribute.UnModifiedPronoun);
        modified.getPhoneticAttributes().add(PhoneticAttribute.ModifiedPronoun);
        return Lists.newArrayList(original, modified);
      default:
        throw new IllegalArgumentException(
            "Lexicon Item with special stem change cannot be handled:" + item);
    }

  }

}
