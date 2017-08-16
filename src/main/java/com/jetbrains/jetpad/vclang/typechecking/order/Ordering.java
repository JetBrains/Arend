package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckableProvider;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProviderSet;

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
  private final InstanceProviderSet myInstanceProviderSet;
  private final TypecheckableProvider myTypecheckableProvider;
  private final DependencyListener myListener;
  private final boolean myRefToHeaders;

  public Ordering(InstanceProviderSet instanceProviderSet, TypecheckableProvider typecheckableProvider, DependencyListener listener, boolean refToHeaders) {
    myInstanceProviderSet = instanceProviderSet;
    myTypecheckableProvider = typecheckableProvider;
    myListener = listener;
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

  public void doOrder(Abstract.Definition definition) {
    if (definition instanceof Abstract.ClassView || definition instanceof Abstract.ClassViewField) {
      return;
    }
    if (!myListener.needsOrdering(definition)) {
      myListener.alreadyTypechecked(definition);
      return;
    }

    Typecheckable typecheckable = new Typecheckable(definition, myRefToHeaders);
    if (!myVertices.containsKey(typecheckable)) {
      doOrderRecursively(typecheckable);
    }
  }

  private enum OrderResult { REPORTED, NOT_REPORTED, RECURSION_ERROR }

  private OrderResult updateState(DefState currentState, Typecheckable dependency) {
    if (!myListener.needsOrdering(dependency.getDefinition())) {
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

    Set<Abstract.GlobalReferableSourceNode> dependencies = new LinkedHashSet<>();
    if (enclosingClass != null) {
      dependencies.add(enclosingClass);
    }

    DependencyListener.Recursion recursion = DependencyListener.Recursion.NO;
    definition.accept(new DefinitionGetDepsVisitor(myInstanceProviderSet.getInstanceProvider(definition), myTypecheckableProvider, dependencies), typecheckable.isHeader());
    if (typecheckable.isHeader() && dependencies.contains(definition)) {
      myStack.pop();
      currentState.onStack = false;
      return OrderResult.RECURSION_ERROR;
    }

    for (Abstract.GlobalReferableSourceNode referable : dependencies) {
      Abstract.Definition dependency = myTypecheckableProvider.forReferable(referable);
      if (dependency instanceof Abstract.ClassField) {
        dependency = ((Abstract.ClassField) dependency).getParentDefinition();
        assert dependency != null;
      } else if (dependency instanceof Abstract.Constructor) {
        dependency = ((Abstract.Constructor) dependency).getDataType();
      }

      if (dependency.equals(definition)) {
        if (!(referable instanceof Abstract.ClassField)) {
          recursion = DependencyListener.Recursion.IN_BODY;
        }
      } else {
        myListener.dependsOn(typecheckable, dependency);
        updateState(currentState, new Typecheckable(dependency, myRefToHeaders));
      }
    }

    SCC scc = null;
    if (currentState.lowLink == currentState.index) {
      List<TypecheckingUnit> units = new ArrayList<>();
      do {
        unit = myStack.pop();
        myVertices.get(unit.getTypecheckable()).onStack = false;
        units.add(unit);
      } while (!unit.getTypecheckable().equals(typecheckable));
      Collections.reverse(units);
      scc = new SCC(units);

      if (myRefToHeaders) {
        myListener.sccFound(scc);
        return OrderResult.REPORTED;
      }

      if (typecheckable.isHeader() && units.size() == 1) {
        return OrderResult.NOT_REPORTED;
      }

      if (units.size() == 1) {
        myListener.unitFound(unit, recursion);
        return OrderResult.REPORTED;
      }
    }

    if (header != null) {
      myListener.sccFound(new SCC(Collections.singletonList(new TypecheckingUnit(header, enclosingClass))));
    }
    if (scc != null) {
      myListener.sccFound(scc);
    }

    return OrderResult.REPORTED;
  }
}
