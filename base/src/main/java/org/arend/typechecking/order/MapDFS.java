package org.arend.typechecking.order;

import java.util.Collection;
import java.util.Map;

public class MapDFS<T> extends DFS<T,Void> {
  private final Map<T, ? extends Collection<? extends T>> myMap;

  public MapDFS(Map<T, ? extends Collection<? extends T>> map) {
    myMap = map;
  }

  @Override
  protected Void forDependencies(T unit) {
    Collection<? extends T> list = myMap.get(unit);
    if (list != null) {
      for (T t : list) {
        visit(t);
      }
    }
    return null;
  }
}
