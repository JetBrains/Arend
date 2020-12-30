package org.arend.typechecking.order;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public abstract class DFS<T> {
  public static class CycleException extends RuntimeException {

  }

  private final Map<T, Boolean> myVisited = new HashMap<>();

  public void visit(T unit) {
    Boolean prev = myVisited.putIfAbsent(unit, false);
    if (prev != null) {
      if (prev || allowCycles()) {
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

  public void visit(Collection<? extends T> units) {
    for (T unit : units) {
      visit(unit);
    }
  }

  protected abstract boolean allowCycles();

  protected abstract void forDependencies(T unit, Consumer<T> consumer);

  public Set<T> getVisited() {
    return myVisited.keySet();
  }

  protected void onEnter(T unit) {

  }

  protected void onExit(T unit) {

  }
}
