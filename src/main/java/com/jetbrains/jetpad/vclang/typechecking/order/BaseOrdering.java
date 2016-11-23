package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

public class BaseOrdering {
  private static class DefState {
    int index, lowLink;
    boolean onStack;

    public DefState(int currentIndex) {
      index = currentIndex;
      lowLink = currentIndex;
      onStack = true;
    }
  }

  private int myIndex = 0;
  private final Stack<SCC.TypecheckingUnit> myStack = new Stack<>();
  private final Map<Abstract.Definition, DefState> myVertices = new HashMap<>();
  private final SCCListener myListener;

  public BaseOrdering(SCCListener listener) {
    myListener = listener;
  }

  protected void dependsOn(Abstract.Definition def1, Abstract.Definition def2) {

  }

  private Abstract.ClassDefinition getEnclosingClass(Abstract.Definition definition) {
    Abstract.Definition parent = definition.getParent();
    if (parent == null) {
      return null;
    }
    if (parent instanceof Abstract.ClassDefinition && !definition.isStatic()) {
      return (Abstract.ClassDefinition) parent;
    }
    return getEnclosingClass(parent);
  }

  private Set<Abstract.Definition> getTypecheckable(Abstract.Definition referable) {
    if (referable instanceof Abstract.ClassField) {
      return Collections.singleton(referable.getParent());
    }
    if (referable instanceof Abstract.Constructor) {
      return Collections.<Abstract.Definition>singleton(((Abstract.Constructor) referable).getDataType());
    }
    if (referable instanceof Abstract.ClassView) {
      return Collections.emptySet();
    }
    return Collections.singleton(referable);
  }

  public void doOrder(final Abstract.Definition definition) {
    if (!myVertices.containsKey(definition)) {
      doOrderRecursively(definition);
    }
  }

  private void doOrderRecursively(final Abstract.Definition definition) {
    Abstract.ClassDefinition enclosingClass = getEnclosingClass(definition);
    DefState currentState = new DefState(myIndex);
    myVertices.put(definition, currentState);
    myIndex++;
    myStack.push(new SCC.TypecheckingUnit(definition, enclosingClass));

    Set<Abstract.Definition> dependencies = new HashSet<>();
    if (enclosingClass != null) {
      dependencies.add(enclosingClass);
    }

    definition.accept(new DefinitionGetDepsVisitor(dependencies), null);
    for (Abstract.Definition referable : dependencies) {
      for (Abstract.Definition dependency : getTypecheckable(referable)) {
        dependsOn(definition, dependency);
        DefState state = myVertices.get(dependency);
        if (state == null) {
          doOrderRecursively(dependency);
          currentState.lowLink = Math.min(currentState.lowLink, myVertices.get(definition).lowLink);
        } else if (state.onStack) {
          currentState.lowLink = Math.min(currentState.lowLink, state.index);
        }
      }
    }

    if (currentState.lowLink == currentState.index) {
      SCC scc = new SCC();
      SCC.TypecheckingUnit unit;
      do {
        unit = myStack.pop();
        myVertices.get(unit.definition).onStack = false;
        scc.add(unit);
      } while (!unit.definition.equals(definition));
      myListener.sccFound(scc);
    }
  }
}
