package zemberek.morphology.lexicon;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.lexicon.proto.LexiconProto;
import zemberek.morphology.lexicon.proto.LexiconProto.Dictionary;

public class Serializer {

  public static void main(String[] args) throws IOException {

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    RootLexicon lexicon = morphology.getLexicon();
    Dictionary.Builder builder = Dictionary.newBuilder();
    for (DictionaryItem item : lexicon.getAllItems()) {
      builder.addItems(convertToProto(item));
    }
    Dictionary dictionary = builder.build();
    System.out.println("Total size of serialized dictionary: " + dictionary.getSerializedSize());
    File f = new File("lexicon.bin");
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
    bos.write(dictionary.toByteArray());
    bos.close();

    long start = System.currentTimeMillis();
    byte[] serialized = Files.readAllBytes(new File("lexicon.bin").toPath());
    long end = System.currentTimeMillis();
    Log.info("Dictionary loaded in %d ms.", (end - start));

    start = System.currentTimeMillis();
    Dictionary readDictionary = Dictionary.parseFrom(serialized);
    end = System.currentTimeMillis();
    Log.info("Dictionary deserialized in %d ms.", (end - start));
    System.out.println("Total size of read dictionary: " + readDictionary.getSerializedSize());

    start = System.currentTimeMillis();
    RootLexicon loadedLexicon = new RootLexicon();
    for (LexiconProto.DictionaryItem item : readDictionary.getItemsList()) {
      loadedLexicon.add(convertToDictionaryItem(item));
    }
    end = System.currentTimeMillis();
    Log.info("RootLexicon generated in %d ms.", (end - start));
  }

  static SimpleEnumConverter<PrimaryPos, LexiconProto.PrimaryPos> primaryPosConverter =
     SimpleEnumConverter.createConverter(PrimaryPos.class, LexiconProto.PrimaryPos.class);

  static SimpleEnumConverter<SecondaryPos, LexiconProto.SecondaryPos> secondaryPosConverter =
      SimpleEnumConverter.createConverter(SecondaryPos.class, LexiconProto.SecondaryPos.class);

  static SimpleEnumConverter<RootAttribute, LexiconProto.RootAttribute> rootAttributeConverter =
      SimpleEnumConverter.createConverter(RootAttribute.class, LexiconProto.RootAttribute.class);

  private static LexiconProto.DictionaryItem convertToProto(DictionaryItem item) {
    LexiconProto.DictionaryItem.Builder builder = LexiconProto.DictionaryItem.newBuilder()
        .setLemma(item.lemma)
        .setPrimaryPos(primaryPosConverter.convertTo(item.primaryPos, LexiconProto.PrimaryPos.PrimaryPos_Unknown));
    String lowercaseLemma = item.lemma.toLowerCase();
    if (item.root != null && !item.root.equals(lowercaseLemma)) {
      builder.setRoot(item.root);
    }
    if (item.pronunciation != null && !item.pronunciation.equals(lowercaseLemma)) {
      builder.setPronunciation(item.pronunciation);
    }
    if (item.secondaryPos != null && item.secondaryPos != SecondaryPos.None) {
      builder.setSecondaryPos(secondaryPosConverter.convertTo(
          item.secondaryPos, LexiconProto.SecondaryPos.SecondaryPos_Unknown));
    }
    if (item.attributes != null && !item.attributes.isEmpty()) {
        builder.addAllRootAttributes(item.attributes
          .stream()
          .map((attribute) ->
              rootAttributeConverter.convertTo(attribute, LexiconProto.RootAttribute.RootAttribute_Unknown))
          .collect(Collectors.toList()));
    }
    return builder.build();
  }

  private static DictionaryItem convertToDictionaryItem(LexiconProto.DictionaryItem item) {
    List<RootAttribute> rootAttributes = new ArrayList<>();
    for (LexiconProto.RootAttribute rootAttribute : item.getRootAttributesList()) {
      rootAttributes.add(rootAttributeConverter.convertBack(rootAttribute, RootAttribute.Unknown));
    }
    String lowercaseLemma = item.getLemma().toLowerCase();
    return new DictionaryItem(item.getLemma(),
        item.getRoot().isEmpty() ? lowercaseLemma : item.getRoot(),
        item.getPronunciation().isEmpty() ? lowercaseLemma : item.getPronunciation(),
        primaryPosConverter.convertBack(item.getPrimaryPos(), PrimaryPos.Unknown),
        item.getSecondaryPos() == LexiconProto.SecondaryPos.SecondaryPos_Unknown
            ? SecondaryPos.None
            : secondaryPosConverter.convertBack(item.getSecondaryPos(), SecondaryPos.Unknown),
        !rootAttributes.isEmpty() ? EnumSet.copyOf(rootAttributes) : null,
        null);
  }

  static class SimpleEnumConverter <E extends Enum<E>, P extends Enum<P>> {
    Map<String, P> conversionFromEToP = new HashMap<>();
    Map<String, E> conversionFromPToE = new HashMap<>();

    private SimpleEnumConverter(Map<String, P> conversionFromEToP,
        Map<String, E> conversionFromPToE) {
      this.conversionFromEToP = conversionFromEToP;
      this.conversionFromPToE = conversionFromPToE;
    }

    public P convertTo (E en, P defaultEnum) {
      P pEnum = conversionFromEToP.get(en.name());
      if (pEnum == null) {
        Log.warn("Could not map from Enum %s Returning default", en.name());
        return defaultEnum;
      }
      return pEnum;
    }

    public E convertBack (P en, E defaultEnum) {
      E eEnum = conversionFromPToE.get(en.name());
      if (eEnum == null) {
        Log.warn("Could not map from Enum %s Returning default", en.name());
        return defaultEnum;
      }
      return eEnum;
    }

    public static  <E extends Enum<E>, P extends Enum<P>> SimpleEnumConverter<E, P>
        createConverter(Class<E> enumType, Class<P> otherEnumType) {
      Map<String, E> namesMapE = createEnumNameMap(enumType);
      Map<String, P> namesMapP = createEnumNameMap(otherEnumType);

      Map<String, P> conversionFromEToP = new HashMap<>();
      Map<String, E> conversionFromPToE = new HashMap<>();
      for (Entry<String, E> entry : namesMapE.entrySet()) {
        if (namesMapP.containsKey(entry.getKey())) {
            conversionFromEToP.put(entry.getKey(), namesMapP.get(entry.getKey()));
        }
      }
      for (Entry<String, P> entry : namesMapP.entrySet()) {
        if (namesMapP.containsKey(entry.getKey())) {
          conversionFromPToE.put(entry.getKey(), namesMapE.get(entry.getKey()));
        }
      }
      return new SimpleEnumConverter<>(conversionFromEToP, conversionFromPToE);
    }

    private static <E extends Enum<E>> Map<String, E> createEnumNameMap(
        Class<E> enumType) {
      Map<String, E> nameToEnum = new HashMap<>();
      // put Enums in map by name
      for (E enumElement : EnumSet.allOf(enumType)) {
           nameToEnum.put(enumElement.name(), enumElement);
      }
      return nameToEnum;
    }
  }

}
