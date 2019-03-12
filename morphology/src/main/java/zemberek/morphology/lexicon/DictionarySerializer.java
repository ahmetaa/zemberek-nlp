package zemberek.morphology.lexicon;

import com.google.common.io.ByteStreams;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import zemberek.core.enums.EnumConverter;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.proto.LexiconProto;
import zemberek.morphology.lexicon.proto.LexiconProto.Dictionary;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class DictionarySerializer {

  static EnumConverter<PrimaryPos, LexiconProto.PrimaryPos> primaryPosConverter =
      EnumConverter.createConverter(PrimaryPos.class, LexiconProto.PrimaryPos.class);
  static EnumConverter<SecondaryPos, LexiconProto.SecondaryPos> secondaryPosConverter =
      EnumConverter.createConverter(SecondaryPos.class, LexiconProto.SecondaryPos.class);
  static EnumConverter<RootAttribute, LexiconProto.RootAttribute> rootAttributeConverter =
      EnumConverter.createConverter(RootAttribute.class, LexiconProto.RootAttribute.class);

  public static RootLexicon loadFromResources(String resourcePathString) throws IOException {
    try (InputStream is = DictionarySerializer.class.getResourceAsStream(resourcePathString)) {
      byte[] bytes = ByteStreams.toByteArray(is);
      return getDictionaryItems(bytes);
    }
  }

  public static RootLexicon load(Path path) throws IOException {
    byte[] bytes = Files.readAllBytes(path);
    return getDictionaryItems(bytes);
  }

  public static void createDefaultDictionary(Path path) throws IOException {
    RootLexicon lexicon = RootLexicon.builder()
        .addTextDictionaryResources(TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES)
        .build();
    save(lexicon, path);
  }

  private static RootLexicon getDictionaryItems(byte[] bytes)
      throws IOException {
    long start = System.currentTimeMillis();
    Dictionary readDictionary = Dictionary.parseFrom(bytes);
    RootLexicon loadedLexicon = new RootLexicon();
    // some items contains references to other items. We need to apply this
    // link after creating the lexicon.
    Map<String, String> referenceItemIdMap = new HashMap<>();
    for (LexiconProto.DictionaryItem item : readDictionary.getItemsList()) {
      DictionaryItem actual = convertToDictionaryItem(item);
      loadedLexicon.add(actual);
      if (item.getReference() != null && !item.getReference().isEmpty()) {
        referenceItemIdMap.put(actual.id, item.getReference());
      }
    }

    for (String itemId : referenceItemIdMap.keySet()) {
      DictionaryItem item = loadedLexicon.getItemById(itemId);
      DictionaryItem ref = loadedLexicon.getItemById(referenceItemIdMap.get(itemId));
      item.setReferenceItem(ref);
    }

    long end = System.currentTimeMillis();
    Log.info("Root lexicon created in %d ms.", (end - start));

    return loadedLexicon;
  }

  public static void save(RootLexicon lexicon, Path outPath) throws IOException {
    Dictionary.Builder builder = Dictionary.newBuilder();
    for (DictionaryItem item : lexicon.getAllItems()) {
      builder.addItems(convertToProto(item));
    }
    Dictionary dictionary = builder.build();
    if (outPath.toFile().exists()) {
      Files.delete(outPath);
    }
    Files.write(outPath, dictionary.toByteArray(), StandardOpenOption.CREATE);
  }

  public static void main(String[] args) throws IOException {
    createDefaultDictionary(Paths.get("morphology/src/main/resources/tr/lexicon.bin"));
    serializeDeserializeTest();
  }

  private static void serializeDeserializeTest() throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    RootLexicon lexicon = morphology.getLexicon();
    Dictionary.Builder builder = Dictionary.newBuilder();
    for (DictionaryItem item : lexicon.getAllItems()) {
      builder.addItems(convertToProto(item));
    }
    Dictionary dictionary = builder.build();
    System.out.println("Total size of serialized dictionary: " + dictionary.getSerializedSize());
    Path f = Files.createTempFile("lexicon", ".bin");
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f.toFile()));
    bos.write(dictionary.toByteArray());
    bos.close();

    long start = System.currentTimeMillis();
    byte[] serialized = Files.readAllBytes(f);
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

  private static LexiconProto.DictionaryItem convertToProto(DictionaryItem item) {
    LexiconProto.DictionaryItem.Builder builder = LexiconProto.DictionaryItem.newBuilder()
        .setLemma(item.lemma)
        .setIndex(item.index)
        .setPrimaryPos(primaryPosConverter
            .convertTo(item.primaryPos, LexiconProto.PrimaryPos.PrimaryPos_Unknown));
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
              rootAttributeConverter
                  .convertTo(attribute, LexiconProto.RootAttribute.RootAttribute_Unknown))
          .collect(Collectors.toList()));
    }
    if (item.getReferenceItem() != null) {
      builder.setReference(item.getReferenceItem().id);
    }
    return builder.build();
  }

  private static DictionaryItem convertToDictionaryItem(LexiconProto.DictionaryItem item) {
    EnumSet<RootAttribute> attributes = EnumSet.noneOf(RootAttribute.class);
    for (LexiconProto.RootAttribute rootAttribute : item.getRootAttributesList()) {
      attributes.add(rootAttributeConverter.convertBack(rootAttribute, RootAttribute.Unknown));
    }
    Locale locale = attributes.contains(RootAttribute.LocaleEn) ? Locale.ENGLISH : Turkish.LOCALE;
    String lowercaseLemma = item.getLemma().toLowerCase(locale);
    return new DictionaryItem(
        item.getLemma(),
        item.getRoot().isEmpty() ? lowercaseLemma : item.getRoot(),
        item.getPronunciation().isEmpty() ? lowercaseLemma : item.getPronunciation(),
        primaryPosConverter.convertBack(item.getPrimaryPos(), PrimaryPos.Unknown),
        item.getSecondaryPos() == LexiconProto.SecondaryPos.SecondaryPos_Unknown
            ? SecondaryPos.None
            : secondaryPosConverter.convertBack(item.getSecondaryPos(), SecondaryPos.UnknownSec),
        attributes,
        item.getIndex());
  }

}
