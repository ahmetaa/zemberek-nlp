package zemberek.core.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zemberek.core.logging.Log;

public class EnumConverter<E extends Enum<E>, P extends Enum<P>> {
  Map<String, P> conversionFromEToP;
  Map<String, E> conversionFromPToE;

  private EnumConverter(
      Map<String, P> conversionFromEToP,
      Map<String, E> conversionFromPToE) {
    this.conversionFromEToP = conversionFromEToP;
    this.conversionFromPToE = conversionFromPToE;
  }

  public static <E extends Enum<E>, P extends Enum<P>> EnumConverter<E, P>
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
    return new EnumConverter<>(conversionFromEToP, conversionFromPToE);
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

  public P convertTo(E en, P defaultEnum) {
    P pEnum = conversionFromEToP.get(en.name());
    if (pEnum == null) {
      Log.warn("Could not map from Enum %s Returning default", en.name());
      return defaultEnum;
    }
    return pEnum;
  }

  public E convertBack(P en, E defaultEnum) {
    E eEnum = conversionFromPToE.get(en.name());
    if (eEnum == null) {
      Log.warn("Could not map from Enum %s Returning default", en.name());
      return defaultEnum;
    }
    return eEnum;
  }
}