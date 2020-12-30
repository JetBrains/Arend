package org.arend.typechecking.order;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public class MapDFS<T> extends DFS<T> {
  private final Map<T, ? extends Collection<? extends T>> myMap;

  public MapDFS(Map<T, ? extends Collection<? extends T>> map) {
    myMap = map;
  }

  @Override
  protected boolean allowCycles() {
    return true;
  }

  @Override
  protected void forDependencies(T unit, Consumer<T> consumer) {
    Collection<? extends T> list = myMap.get(unit);
    if (list != null) {
      for (T t : list) {
        consumer.accept(t);
      }
    }
  }
}
