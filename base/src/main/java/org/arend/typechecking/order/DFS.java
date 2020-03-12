package org.arend.typechecking.order;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class DFS<T> {
  public static class CycleException extends RuntimeException {

  }

  private final Map<T, Boolean> myVisited = new HashMap<>();

  public void visit(T unit) {
    Boolean prev = myVisited.putIfAbsent(unit, false);
    if (prev != null) {
      if (prev) {
        return;
      } else {
        throw new CycleException();
      }
    }

    onEnter(unit);
    forDependencies(unit, this::visit);
    onExit(unit);
    myVisited.put(unit, true);
  }

  protected abstract void forDependencies(T unit, Consumer<T> consumer);

  protected void onEnter(T unit) {

  }

  protected void onExit(T unit) {

  }
}
