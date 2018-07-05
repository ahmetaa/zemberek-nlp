package zemberek.morphology.morphotactics;

import zemberek.core.collections.IntMap;

/**
 * A cache for set of morphemic attributes to surface forms.
 *
 * For thread safety, writes to map are synchronized, when map needs expanding
 * writer thread creates the expanded version and replaces the map. The map
 * reference is volatile so readers always see a consistent albeit possibly
 * stale version of the map.
 *
 * This is also knows as cheap read-write lock trick (see item #5 in the link)
 * https://www.ibm.com/developerworks/java/library/j-jtp06197/index.html
 */
 class AttributeToSurfaceCache {

  // volatile guarantees atomic reference copy.
  private volatile IntMap<String> attributeMap;

  AttributeToSurfaceCache() {
    attributeMap = IntMap.createManaged();
  }

  synchronized void addSurface(int attributes, String surface) {
    while (!attributeMap.put(attributes, surface)) {
      attributeMap = attributeMap.expand();
    }
  }

  String getSurface(int attributes) {
    IntMap<String> map = attributeMap;
    return map.get(attributes);
  }
}
