package zemberek.ner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

/**
 * Post processes named entities by removing suffixes from last word.
 * TODO: requires some refactoring.
 *
 * Originally written by Ayça Müge Sevinç. *
 */

public class NEPostProcessor {

  public String type;
  public String wordList[];
  private String lastWord;

  private int relKiCount;

  private String longestLemma;
  private String longFormatforLongestLemma;
  private String longestNLemma;
  private String longFormatforNLongestLemma;
  private String longestNStem;
  private String longFormatforLongestNStem;

  public NamedEntity orginalNE;
  public NamedEntity postProcessedNE;

  public NEPostProcessor(NamedEntity namedEntity) {

    longestLemma = "";
    longFormatforLongestLemma = "";
    longestNLemma = "";
    longFormatforNLongestLemma = "";
    longestNStem = "";
    longFormatforLongestNStem = "";
    wordList = namedEntity.content().split(" ");
    lastWord = wordList[wordList.length - 1];
    type = namedEntity.type;
    relKiCount = 0;
    orginalNE = namedEntity;
    postProcessedNE = namedEntity;

  }

  /// Lemmas are lower cased, but we need the named entities not lower-cased, so we upper-case them when needed.
  private String capitalize(String lemma) {
    if (lemma.length() == 0) {
      return lemma;
    }
    String first = lemma.substring(0, 1).toUpperCase(Turkish.LOCALE);
    return lemma.length() < 2 ? first : first + lemma.substring(1);
  }

  /// Check if there is  apostrophe in the last word,
  // if so remove the part including and following it from the namedEntity
  private NamedEntity apostropheRemoved() {

    String apostropheRemoved = "";
    if (lastWord.contains("\'")) {
      apostropheRemoved = lastWord.substring(0, lastWord.indexOf('\''));
    }
    if (lastWord.contains("’")) {
      apostropheRemoved = lastWord.substring(0, lastWord.indexOf('’'));
    }
    postProcessedNE = updateLastWord(orginalNE, apostropheRemoved);
    return postProcessedNE;
  }

  /// Update the last word of a given named entity
  private NamedEntity updateLastWord(NamedEntity namedEntity, String lastWord) {
    boolean boolLastWord = false;
    List<NerToken> tokens = new ArrayList<>(wordList.length);

    for (int i = 0; i < wordList.length; i++) {
      String s = wordList[i];
      NePosition position;
      if (wordList.length == 1) {
        position = NePosition.UNIT;
        boolLastWord = true;
      } else if (i == 0) {
        position = NePosition.BEGIN;
      } else if (i == wordList.length - 1) {
        position = NePosition.LAST;
        boolLastWord = true;
      } else {
        position = NePosition.INSIDE;
      }
      if (boolLastWord) {
        tokens.add(new NerToken(i, lastWord, type, position));
      } else {
        tokens.add(new NerToken(i, s, type, position));
      }
    }
    return new NamedEntity(type, tokens);
  }

  /// This method checks the derived forms to point out the parses that we want to skip.
  /// It returns true if the derived form is derived from a nominal with the incorrect possesive form, o if it is non-predicate form or if it is a derived nominal with incorrect possesives
  /// It also strips the adjectival suffix -ki
  private boolean derivedForms(String longFormat) {

    // check if it is a derived form
    if (longFormat.contains("|")) {
      //find the last inflectional group
      String lastIG = longFormat.substring(longFormat.lastIndexOf("|") + 1);

      //find the first inflectional group
      String firstIG = longFormat.substring(1, longFormat.indexOf("|"));
      firstIG = firstIG.substring(firstIG.indexOf("] ") + 1);
      firstIG = firstIG.substring(firstIG.indexOf(":") + 1);

      //  check whether its final IG is a noun, or it is a predicate derived from a noun.
      // If these two does not hold, skip that parse.
      boolean isLastIGNominalPossessive3rdPerson =
          lastIG.contains("Noun") && (lastIG.contains("P2sg") || lastIG.contains("P2pl") ||
              lastIG.contains("P1pl") ||
              lastIG.contains("P1sg"));
      boolean isFirstIGNominalPossessive3rdPerson =
          firstIG.contains("Noun") && (firstIG.contains("P2sg") ||
              firstIG.contains("P2pl") ||
              firstIG.contains("P1pl") ||
              firstIG.contains("P1sg"));

      if (isFirstIGNominalPossessive3rdPerson && lastIG.contains("Verb")) {
        return true;
      }

      if (isLastIGNominalPossessive3rdPerson) {
        return true;
      }

      // if it is derived adjective (rel ki) from the nominal, remove those IGs
      // Ex: Izmirdeki,  [İzmir:Noun,Prop] izmir:Noun+A3sg+de:Loc|ki:Rel→Adj --> [İzmir:Noun,Prop] izmir:Noun+A3sg+de:Loc
      while (longFormat.contains("ki:Rel→")) {
        longFormat = longFormat.substring(0, longFormat.lastIndexOf("|"));
        if (longFormat.contains("|")) {
          lastIG = longFormat.substring(longFormat.lastIndexOf("|") + 1);
        } else {
          lastIG = firstIG;
        }
        relKiCount++;
      }
    }
    return false;
  }

  // This method returns true if it is not a nominal or if it is a nominal with incorrect possesive markers
  private boolean unInflectedNominalForm(String longFormat) {

    // if it is not derived form, check if it is a noun and also check if the appropriate
    // possessive forms , i.e., Pnon, P3sg, P3pl
    boolean isNomimalPossesive3rdPerson =
        longFormat.contains("Noun") && (longFormat.contains("P2sg") || longFormat.contains("P2pl")
            || longFormat.contains("P1pl") || longFormat.contains("P1sg"));

    if (!longFormat.contains("Noun")) {
      return true;
    }
    if (isNomimalPossesive3rdPerson) {
      return true;
    }

    return false;
  }

  /// This method goes over all the possible parses for the last word of the named entity,
  //  it skips the irrelevant parses for the NE in the list of all morphological parses.
  /// It finds the longest lemmas for the relevant parses which is either the proper noun reading and common noun reading.
  /// Relevant parses also includes the predicate forms which are derived from the nouns.
  /// Then, it processes the required suffix stripping on the last word depending the named entity's type

  private NamedEntity MorphologicalAnalysisForNamedEntity(TurkishMorphology morphology) {
    {
      //get all the morphological analysis for the last word in the namedEntity
      WordAnalysis results = morphology.analyze(lastWord);

      // if there are no parses returned for the morphological analyses, it means that
      // the lastWord is unknown, hence return the namedEntity without changing it
      if (results.analysisCount() == 0) {
        //return the the original namedEntity
        return orginalNE;
      }

      //we will focus on only the analyses for nominals or the predicate of the nominal forms
      for (SingleAnalysis result : results) {

        String longFormat = result.formatLong();
        // if (log) System.out.print("longFormat:  " + longFormat + '\n');

        if (derivedForms(longFormat)) {
          // skip the parses which contain non-predicate forms, or a derived nominal
          // with incoorect possesives, or a form derived from a nominal with the incorrect possesive markers
          continue;
        } else if (unInflectedNominalForm(longFormat)) {
          continue; // skip the parses which are not nominals or which are nominals with incorrect possesive markers
        }

        // get the list of possible lemmas for the lastWord
        List<String> ListOfLemmas = result.getLemmas();
        List<String> ListOfStems = result.getStems();

        // if the list of lemmas is empty, return the original namedEntity
        if (ListOfLemmas == null || ListOfLemmas.isEmpty()) {
          return orginalNE;
        }

        // Get the last lemma on the lemma list since it is the longest one
        String LastLemma = ListOfLemmas.get(ListOfLemmas.size() - 1);
        String LastStem = ListOfStems.get(ListOfStems.size() - 1);

        // Check if the last lemma in the list of lemmas is a derived adjective
        // (with the suffix rel -ki, if so, strip that suffix, then continue checking with the next
        // longest lemma in the list
        // ki is recursive it may be added many times: izmirdeki, istanbuldakilerinki, ...
        int x = 0;
        while (relKiCount > 0 && LastLemma.endsWith("ki") && LastStem.endsWith("ki")) {
          LastLemma = ListOfLemmas.get(ListOfLemmas.size() - 2 - x);
          LastStem = ListOfStems.get(ListOfStems.size() - 2 - x);
          relKiCount--;
          x++;
        }

        // Uppercase the lemma, we need the original form in the NE
        String CLastLemma = capitalize(LastLemma);
        String CLastStem = capitalize(LastStem);

        // if CLastLemma for the proper noun is longer than the previous longest lemma of any
        // previous parses, then update longestLemma
        if (longestLemma.length() <= CLastLemma.length() && (longFormat.contains("Noun,Prop]")
            || longFormat.contains("Noun,Abbrv]"))) {
          longestLemma = CLastLemma;
          longFormatforLongestLemma = longFormat;
        }

        // if CLastLemma for the common noun is longer than the previous longest lemma
        // for the noun of any previous parses, then update longestNLemma
        if (longestNLemma.length() <= CLastLemma.length() && longFormat.contains("Noun]")) {
          longestNLemma = CLastLemma;
          longFormatforNLongestLemma = longFormat;
        }
        // for any kind of noun update the longestNStem
        if (longestNStem.length() <= CLastStem.length() && longFormat.contains("Noun]")) {
          longestNStem = CLastStem;
          longFormatforLongestNStem = longFormat;
        }

        // if it is an abbreviation, its letters should be all capitalized
        if (longFormat.contains("Noun,Abbrv]") || longFormat.contains("Noun,Prop]")) {
          int ok = 1;
          for (int i = 0; i < 2 && i < longestLemma.length(); i++) {
            //check if the characters are upper cased in the original namedEntity
            if (!Character.isUpperCase(lastWord.charAt(i))) {
              ok = 0;
              break;
            }
          }
          if (ok == 1) {
            longestLemma = longestLemma.toUpperCase();
          }
        }
      }

      // in the case of there is no proper noun reading, make use of the common noun reading
      if (longestLemma.length() == 0 && longestNLemma.length() > 0) {
        longestLemma = longestNLemma;
        longFormatforLongestLemma = longFormatforNLongestLemma;
      }
      // if there are no parses left after skipping the irrelevant ones,
      // then return the original namedEntity
      if (longestLemma.length() == 0 && longestNLemma.length() == 0) {
        return orginalNE; //return the same as the input named entity
      }

      // Choose one of the above determined longest lemmas based on the named entity type:
      // PERSON, ORGANIZATION, LOCATION
      postProcessedNE = chooseBestLemmaBasedOnNamedEntityType();
      return postProcessedNE;
    }
  }

  // Based on the named entity type (PERSON, ORGANIZATION, LOCATION),
  // it decides the best lemma to choose
  private NamedEntity chooseBestLemmaBasedOnNamedEntityType() {

    // if there is no  apostrophe and there is only one word in ner
    // (Kaan, Kaana, istanbulda, Komutanlıgı) return the longest lemma
    if (wordList.length == 1) {
      postProcessedNE = updateLastWord(orginalNE, longestLemma);
      return postProcessedNE;
    }
    // if there is no  apostrophe and there are more than one word in NE
    // (Toros Daglarinda, Kaan Irmak, Kaan Irmagı)
    else {
      // When the type is person we expect it to be in an uninflected form in NE,
      // so we just return the longest lemma (without any inflections)
      //Ex: deneyim, birligi
      if (type.equals("PERSON")) {
        postProcessedNE = updateLastWord(orginalNE, longestLemma);
        return postProcessedNE;
      }

      // When the type is organization or location,  we return the longest nominal lemma
      // (with the inflections p3sg or p3pl)
      else if (type.equals("ORGANIZATION") || type.equals("LOCATION")) { //organization or location
        String lemmaPos = longFormatforLongestLemma
            .substring(0, longFormatforLongestLemma.indexOf(']'));
        String lemma = lemmaPos.substring(1, lemmaPos.indexOf(':'));
        String pos = lemmaPos.substring(lemmaPos.indexOf(':') + 1, lemmaPos.length());

        // if we have a common noun reading in the analysis, we use it for organization & location
        if (!longFormatforNLongestLemma.equals("")) {
          // if the last word  is a common noun
          String nLemmaPos = longFormatforNLongestLemma
              .substring(0, longFormatforNLongestLemma.indexOf(']'));
          String nCat = longFormatforNLongestLemma
              .substring(longFormatforNLongestLemma.indexOf(' '));
          nCat = nCat.substring(nCat.indexOf(':') + 1);
          // Birlik vs Bir, Kurulu vs kuru
          // check for the regular expression with singular possesive suffix
          if (nCat.contains("Noun+A3sg+") && nCat.contains(":P3sg")) {
            String suffix = nCat.substring(nCat.indexOf("Noun+A3sg+") + "Noun+A3sg+".length(),
                nCat.indexOf(":P3sg"));
            postProcessedNE = updateLastWord(orginalNE, longestNStem + suffix);
            return postProcessedNE;

          }
          // check for the regular expression like -leri, ları
          Pattern pattern = Pattern.compile("Noun\\+l.r:A3pl\\+.*:P3.*");
          Matcher matcher = pattern.matcher(nCat);

          if (matcher.find()) {
            String suffix1 = nCat
                .substring(nCat.indexOf("Noun+") + "Noun+".length(), nCat.indexOf(":A3pl"));
            String suffix2 = nCat
                .substring(nCat.indexOf("r:A3pl") + "r:A3pl+".length(), nCat.indexOf(":P3"));
            postProcessedNE = updateLastWord(orginalNE, longestNStem + suffix1 + suffix2);
            return postProcessedNE;

          }
        }
        // if there is no common noun reading for the location or organization,
        // then we use the proper noun reading
        else if (longestNLemma.equals("") &&
            (pos.equals("Noun,Prop") || pos.equals("Noun,Abbrv")) &&
            lemma.equals(longestLemma)) {

          postProcessedNE = updateLastWord(orginalNE, longestLemma);
          return postProcessedNE;
        }

      }
      return orginalNE;
    }
  }

  /// This method post-process the named entity to strip any suffixes that are attached to them
  public NamedEntity postProcessNER(TurkishMorphology morphology) {

    // check if there is  apostrophe in the last word,
    // if so remove the part including and following it from the namedEntity
    if (lastWord.contains("\'") || lastWord.contains("’")) {
      return apostropheRemoved();
    }

    // check for the best nominal analysis depending on the type of the namedEntity,
    // strip the suffixes on the named entity
    return MorphologicalAnalysisForNamedEntity(morphology);
  }
}


