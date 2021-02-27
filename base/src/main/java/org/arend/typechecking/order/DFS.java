package org.arend.typechecking.order;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class DFS<T,R> {
  public static class CycleException extends RuntimeException {

  }

  private final Map<T, Boolean> myVisited = new HashMap<>();

  public R visit(T unit) {
    Boolean prev = myVisited.putIfAbsent(unit, false);
    if (prev != null) {
      if (prev || allowCycles()) {
        return getVisitedValue(unit, !prev);
      } else {
        throw new CycleException();
      }
    }

    R result = forDependencies(unit);
    myVisited.put(unit, true);
    return result;
  }

  public void visit(Collection<? extends T> units) {
    for (T unit : units) {
      visit(unit);
    }
  }

  protected boolean allowCycles() {
    return true;
  }

  protected R getVisitedValue(T unit, boolean cycle) {
    return null;
  }

  protected abstract R forDependencies(T unit);

  public Set<T> getVisited() {
    return myVisited.keySet();
  }
}
