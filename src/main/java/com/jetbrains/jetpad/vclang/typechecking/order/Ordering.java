package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.ClassViewInstanceProvider;

import java.util.*;

public class Ordering {
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
  private final Stack<TypecheckingUnit> myStack = new Stack<>();
  private final Map<Typecheckable, DefState> myVertices = new HashMap<>();
  private final ClassViewInstanceProvider myInstanceProvider;
  private final DependencyListener myListener;

  public static class SCCException extends RuntimeException {
    public SCC scc;

    public SCCException(SCC scc) {
      this.scc = scc;
    }
  }

  public Ordering(ClassViewInstanceProvider instanceProvider, DependencyListener listener) {
    myInstanceProvider = instanceProvider;
    myListener = listener;
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

  private Collection<? extends Abstract.Definition> getTypecheckable(Abstract.Definition referable, Abstract.ClassDefinition enclosingClass) {
    if (referable instanceof Abstract.ClassViewField) {
      referable = ((Abstract.ClassViewField) referable).getUnderlyingField();
    }

    if (referable instanceof Abstract.ClassField) {
      return Collections.singletonList(referable.getParent());
    }

    if (referable instanceof Abstract.Constructor) {
      return Collections.singletonList(((Abstract.Constructor) referable).getDataType());
    }

    if (referable instanceof Abstract.ClassView) {
      return Collections.singletonList(((Abstract.ClassView) referable).getUnderlyingClassDefCall().getReferent());
    }

    if (referable instanceof Abstract.ClassDefinition && !referable.equals(enclosingClass)) {
      Collection<? extends Abstract.Definition> instanceDefinitions = ((Abstract.ClassDefinition) referable).getInstanceDefinitions();
      List<Abstract.Definition> result = new ArrayList<>(instanceDefinitions.size() + 1);
      result.add(referable);
      result.addAll(instanceDefinitions);
      return result;
    }

    return Collections.singletonList(referable);
  }

  public void doOrder(Abstract.Definition definition) {
    if (!(definition instanceof Abstract.ClassView) && !(definition instanceof Abstract.ClassViewField)) {
      Typecheckable typecheckable = new Typecheckable(definition, false);
      if (!myVertices.containsKey(typecheckable)) {
        doOrderRecursively(typecheckable);
      }
    }
  }

  private void updateState(DefState currentState, Typecheckable dependency) {
    DefState state = myVertices.get(dependency);
    if (state == null) {
      doOrderRecursively(dependency);
      currentState.lowLink = Math.min(currentState.lowLink, myVertices.get(dependency).lowLink);
    } else if (state.onStack) {
      currentState.lowLink = Math.min(currentState.lowLink, state.index);
    }
  }

  private void doOrderRecursively(Typecheckable typecheckable) {
    Abstract.Definition definition = typecheckable.getDefinition();
    Abstract.ClassDefinition enclosingClass = getEnclosingClass(definition);
    TypecheckingUnit unit = new TypecheckingUnit(typecheckable, enclosingClass);
    DefState currentState = new DefState(myIndex);
    myVertices.put(typecheckable, currentState);
    myIndex++;
    myStack.push(unit);

    Set<Abstract.Definition> dependencies = new HashSet<>();
    if (enclosingClass != null) {
      dependencies.add(enclosingClass);
    }
    if (!typecheckable.isHeader() && Typecheckable.hasHeader(definition)) {
      updateState(currentState, new Typecheckable(definition, true));
    }

    definition.accept(new DefinitionGetDepsVisitor(dependencies), typecheckable.isHeader());
    for (Abstract.Definition referable : dependencies) {
      for (Abstract.Definition dependency : getTypecheckable(referable, enclosingClass)) {
        if (dependency.equals(definition)) {
          if (typecheckable.isHeader()) {
            SCC scc = new SCC();
            scc.add(unit);
            throw new SCCException(scc);
          }
        } else {
          myListener.dependsOn(typecheckable, dependency);
          updateState(currentState, new Typecheckable(dependency, false));
        }
      }
    }

    if (currentState.lowLink == currentState.index) {
      SCC scc = new SCC();
      do {
        unit = myStack.pop();
        myVertices.get(unit.getTypecheckable()).onStack = false;
        scc.add(unit);
      } while (!unit.getDefinition().equals(definition));
      myListener.sccFound(scc);
    }
  }
}
