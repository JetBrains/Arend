package org.arend.util;

import java.util.*;

public class GraphClosure<V> {
  private final Map<V, List<V>> myEdges = new HashMap<>();

  public void addDirected(V item1, V item2) {
    myEdges.computeIfAbsent(item1, v -> new ArrayList<>()).add(item2);
  }

  public void addSymmetric(V item1, V item2) {
    myEdges.computeIfAbsent(item1, v -> new ArrayList<>()).add(item2);
    myEdges.computeIfAbsent(item2, v -> new ArrayList<>()).add(item1);
  }

  private boolean isReachable(V start, V end, Set<V> visited) {
    if (!visited.add(start)) {
      return false;
    }

    if (start.equals(end)) {
      return true;
    }

    List<V> vertices = myEdges.get(start);
    if (vertices == null) {
      return false;
    }

    for (V vertex : vertices) {
      if (isReachable(vertex, end, visited)) {
        return true;
      }
    }

    return false;
  }

  public boolean areEquivalent(V item1, V item2) {
    return isReachable(item1, item2, new HashSet<>());
  }
}
