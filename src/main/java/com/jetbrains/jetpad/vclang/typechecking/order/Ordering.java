package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
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
  private final TypecheckerState myTypecheckerState;
  private final boolean myRefToHeaders;

  public Ordering(ClassViewInstanceProvider instanceProvider, DependencyListener listener, TypecheckerState typecheckerState, boolean refToHeaders) {
    myInstanceProvider = instanceProvider;
    myListener = listener;
    myTypecheckerState = typecheckerState;
    myRefToHeaders = refToHeaders;
  }

  private Abstract.ClassDefinition getEnclosingClass(Abstract.Definition definition) {
    Abstract.Definition parent = definition.getParentDefinition();
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
      return Collections.singletonList(referable.getParentDefinition());
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
    if (definition instanceof Abstract.ClassView || definition instanceof Abstract.ClassViewField) {
      return;
    }
    Definition typechecked = myTypecheckerState.getTypechecked(definition);
    if (typechecked != null && !typechecked.status().needsTypeChecking()) {
      myListener.alreadyTypechecked(typechecked);
      return;
    }

    Typecheckable typecheckable = new Typecheckable(definition, myRefToHeaders);
    if (!myVertices.containsKey(typecheckable)) {
      doOrderRecursively(typecheckable);
    }
  }

  private enum OrderResult { REPORTED, NOT_REPORTED, RECURSION_ERROR }

  private OrderResult updateState(DefState currentState, Typecheckable dependency) {
    Definition typechecked = myTypecheckerState.getTypechecked(dependency.getDefinition());
    if (typechecked != null && !typechecked.status().needsTypeChecking()) {
      return OrderResult.REPORTED;
    }

    OrderResult ok = OrderResult.REPORTED;
    DefState state = myVertices.get(dependency);
    if (state == null) {
      ok = doOrderRecursively(dependency);
      currentState.lowLink = Math.min(currentState.lowLink, myVertices.get(dependency).lowLink);
    } else if (state.onStack) {
      currentState.lowLink = Math.min(currentState.lowLink, state.index);
    }
    return ok;
  }

  private OrderResult doOrderRecursively(Typecheckable typecheckable) {
    Abstract.Definition definition = typecheckable.getDefinition();
    Abstract.ClassDefinition enclosingClass = getEnclosingClass(definition);
    TypecheckingUnit unit = new TypecheckingUnit(typecheckable, enclosingClass);
    DefState currentState = new DefState(myIndex);
    myVertices.put(typecheckable, currentState);
    myIndex++;
    myStack.push(unit);

    Typecheckable header = null;
    if (!typecheckable.isHeader() && Typecheckable.hasHeader(definition)) {
      header = new Typecheckable(definition, true);
      OrderResult result = updateState(currentState, header);

      if (result == OrderResult.RECURSION_ERROR) {
        myStack.pop();
        currentState.onStack = false;
        myListener.unitFound(unit, DependencyListener.Recursion.IN_HEADER);
        return OrderResult.REPORTED;
      }

      if (result == OrderResult.REPORTED) {
        header = null;
      }
    }

    Set<Abstract.Definition> dependencies = new HashSet<>();
    if (enclosingClass != null) {
      dependencies.add(enclosingClass);
    }

    DependencyListener.Recursion recursion = DependencyListener.Recursion.NO;
    definition.accept(new DefinitionGetDepsVisitor(myInstanceProvider, dependencies), typecheckable.isHeader());
    if (typecheckable.isHeader() && dependencies.contains(definition)) {
      myStack.pop();
      currentState.onStack = false;
      return OrderResult.RECURSION_ERROR;
    }

    for (Abstract.Definition referable : dependencies) {
      for (Abstract.Definition dependency : getTypecheckable(referable, enclosingClass)) {
        if (dependency.equals(definition)) {
          recursion = DependencyListener.Recursion.IN_BODY;
        } else {
          myListener.dependsOn(typecheckable, dependency);
          updateState(currentState, new Typecheckable(dependency, myRefToHeaders));
        }
      }
    }

    SCC scc = null;
    if (currentState.lowLink == currentState.index) {
      scc = new SCC();
      do {
        unit = myStack.pop();
        myVertices.get(unit.getTypecheckable()).onStack = false;
        scc.add(unit);
      } while (!unit.getTypecheckable().equals(typecheckable));

      if (myRefToHeaders) {
        myListener.sccFound(scc);
        return OrderResult.REPORTED;
      }
      if (typecheckable.isHeader() && scc.getUnits().size() == 1) {
        return OrderResult.NOT_REPORTED;
      }
      if (scc.getUnits().size() == 1) {
        myListener.unitFound(unit, recursion);
        return OrderResult.REPORTED;
      }
    }

    if (header != null) {
      SCC headerSCC = new SCC();
      headerSCC.add(new TypecheckingUnit(header, enclosingClass));
      myListener.sccFound(headerSCC);
    }
    if (scc != null) {
      myListener.sccFound(scc);
    }

    return OrderResult.REPORTED;
  }
}
