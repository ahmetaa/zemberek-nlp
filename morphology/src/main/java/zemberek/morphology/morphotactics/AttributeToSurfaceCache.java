package zemberek.morphology.morphotactics;

/**
 * A cache for set of morphemic attributes to surface forms.
 *
 * Surface generation only depends on the lowest 7 PhoneticAttribute bits (ordinals 0..6:
 * LastLetterVowel through LastLetterVoiceless), so a fixed 128-element array indexed by
 * {@code attributes & 0x7f} provides lock-free O(1) lookup.
 */
class AttributeToSurfaceCache {

  // Mask for PhoneticAttribute ordinals 0..6 used during surface generation.
  private static final int ATTRIBUTE_MASK = 0x7f;
  private final String[] attributeMap = new String[ATTRIBUTE_MASK + 1];

  void addSurface(int attributes, String surface) {
    attributeMap[attributes & ATTRIBUTE_MASK] = surface;
  }

  String getSurface(int attributes) {
    return attributeMap[attributes & ATTRIBUTE_MASK];
  }
}
