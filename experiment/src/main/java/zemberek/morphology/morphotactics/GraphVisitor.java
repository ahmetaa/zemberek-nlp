package zemberek.morphology.morphotactics;

import java.util.List;

//TODO: fix class and method name
public interface GraphVisitor {

  boolean containsKey(String key);

  boolean containsTailSequence(List<String> keys);
}
