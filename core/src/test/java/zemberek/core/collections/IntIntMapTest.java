package zemberek.core.collections;

public class IntIntMapTest extends IntMapTestBase {

  @Override
  IntIntMapBase createMap() {
    return new IntIntMap();
  }

  @Override
  IntIntMapBase createMap(int initialSize) {
    return new IntIntMap(initialSize);
  }
}
