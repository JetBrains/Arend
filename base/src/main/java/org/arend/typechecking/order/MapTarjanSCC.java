package org.arend.typechecking.order;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MapTarjanSCC<T> extends TarjanSCC<T> {
  private final Map<T, List<T>> myMap;

  public MapTarjanSCC(Map<T, List<T>> map) {
    myMap = map;
  }

  public void order() {
    for (T unit : myMap.keySet()) {
      order(unit);
    }
  }

  @Override
  protected boolean forDependencies(T unit, Consumer<T> consumer) {
    boolean withLoops = false;
    List<T> list = myMap.get(unit);
    if (list != null) {
      for (T t : list) {
        consumer.accept(t);
        if (!withLoops && unit.equals(t)) {
          withLoops = true;
        }
      }
    }
    return withLoops;
  }
}
