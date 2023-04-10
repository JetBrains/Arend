package org.arend.term.prettyprint;

import java.util.HashMap;
import java.util.Map;

public class VariableTracker<V> {
  private final Map<V, Integer> myVariables = new HashMap<>();

  public int getIndex(V var) {
    Integer index = myVariables.get(var);
    if (index != null) {
      return index;
    }

    int num = myVariables.size() + 1;
    myVariables.put(var, num);
    return num;
  }
}
