package org.arend.typechecking.order;

import java.util.*;
import java.util.function.Consumer;

public abstract class BellmanFord<T> {
  private static class DefState {
    int index, lowLink;
    boolean onStack;

    DefState(int currentIndex) {
      index = currentIndex;
      lowLink = currentIndex;
      onStack = true;
    }
  }

  private int myIndex = 0;
  private final Stack<T> myStack = new Stack<>();
  private final Map<T, DefState> myVertices = new HashMap<>();

  public void order(T unit) {
    if (!myVertices.containsKey(unit)) {
      doOrderRecursively(unit);
    }
  }

  private void doOrderRecursively(T unit) {
    DefState currentState = new DefState(myIndex);
    myVertices.put(unit, currentState);
    myIndex++;
    myStack.push(unit);

    boolean withLoops = forDependencies(unit, dependency -> {
      DefState state = myVertices.get(dependency);
      if (state == null) {
        doOrderRecursively(dependency);
        currentState.lowLink = Math.min(currentState.lowLink, myVertices.get(dependency).lowLink);
      } else if (state.onStack) {
        currentState.lowLink = Math.min(currentState.lowLink, state.index);
      }
    });

    if (currentState.lowLink == currentState.index) {
      T originalUnit = unit;
      List<T> scc = new ArrayList<>();
      do {
        unit = myStack.pop();
        myVertices.get(unit).onStack = false;
        scc.add(unit);
      } while (!unit.equals(originalUnit));

      if (scc.size() == 1) {
        unitFound(unit, withLoops);
      } else {
        Collections.reverse(scc);
        sccFound(scc);
      }
    }
  }

  protected abstract boolean forDependencies(T unit, Consumer<T> consumer);

  protected void unitFound(T unit, boolean withLoops) {

  }

  protected void sccFound(List<T> scc) {

  }
}
