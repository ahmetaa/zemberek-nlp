package zemberek.core.collections;

public class CompactIntMapTest extends IntMapTestBase {

  @Override
  IntIntMapBase createMap() {
    return new CompactIntMap();
  }

  @Override
  IntIntMapBase createMap(int initialSize) {
    return new CompactIntMap(initialSize);
  }
}
