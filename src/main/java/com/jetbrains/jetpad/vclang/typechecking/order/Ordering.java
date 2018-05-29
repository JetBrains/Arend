package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProvider;
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
  private final Map<TypecheckingUnit, DefState> myVertices = new HashMap<>();
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private final Typechecking myTypechecking;
  private final boolean myRefToHeaders;

  public Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, Typechecking typechecking, boolean refToHeaders) {
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myTypechecking = typechecking;
    myRefToHeaders = refToHeaders;
  }

  public void doOrder(Concrete.Definition definition) {
    TypecheckingUnit typecheckingUnit = new TypecheckingUnit(definition, myRefToHeaders);
    if (!myVertices.containsKey(typecheckingUnit)) {
      doOrderRecursively(typecheckingUnit);
    }
  }

  private enum OrderResult { REPORTED, NOT_REPORTED, RECURSION_ERROR }

  private OrderResult updateState(DefState currentState, TypecheckingUnit dependency) {
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

  private void collectInstances(InstanceProvider instanceProvider, Set<TCReferable> referables, Set<TCReferable> result) {
    /* TODO[classes]
    while (!referables.isEmpty()) {
      GlobalReferable referable = referables.pop();
      if (result.contains(referable)) {
        continue;
      }
      result.add(referable);

      Concrete.ReferableDefinition definition = myConcreteProvider.getConcrete(referable);
      if (definition instanceof Concrete.ClassSynonymField) {
        for (Concrete.Instance instance : instanceProvider.getInstances(((Concrete.ClassSynonymField) definition).getRelatedDefinition())) {
          referables.push(instance.getData());
        }
      } else if (definition != null) {
        Collection<? extends Concrete.Parameter> parameters = Concrete.getParameters(definition);
        if (parameters != null) {
          for (Concrete.Parameter parameter : parameters) {
            Concrete.ClassSynonym classSynonym = Concrete.getUnderlyingClassView(((Concrete.TypeParameter) parameter).getType());
            if (classSynonym != null) {
              for (Concrete.Instance instance : instanceProvider.getInstances(classSynonym)) {
                referables.push(instance.getData());
              }
            }
          }
        }
      }
    }
    */
  }

  private OrderResult doOrderRecursively(TypecheckingUnit unit) {
    Concrete.Definition definition = unit.getDefinition();
    DefState currentState = new DefState(myIndex);
    myVertices.put(unit, currentState);
    myIndex++;
    myStack.push(unit);

    TypecheckingUnit header = null;
    if (!unit.isHeader() && TypecheckingUnit.hasHeader(definition)) {
      header = new TypecheckingUnit(definition, true);
      OrderResult result = updateState(currentState, header);

      if (result == OrderResult.RECURSION_ERROR) {
        myStack.pop();
        currentState.onStack = false;
        myTypechecking.unitFound(unit, Typechecking.Recursion.IN_HEADER);
        return OrderResult.REPORTED;
      }

      if (result == OrderResult.REPORTED) {
        header = null;
      }
    }

    Set<TCReferable> dependenciesWithoutInstances = new LinkedHashSet<>();
    if (definition.enclosingClass != null) {
      dependenciesWithoutInstances.add(definition.enclosingClass);
    }

    Typechecking.Recursion recursion = Typechecking.Recursion.NO;
    definition.accept(new DefinitionGetDependenciesVisitor(dependenciesWithoutInstances), unit.isHeader());
    Set<TCReferable> dependencies;
    InstanceProvider instanceProvider = myInstanceProviderSet.getInstanceProvider(definition.getData());
    if (instanceProvider == null) {
      dependencies = dependenciesWithoutInstances;
    } else {
      dependencies = new LinkedHashSet<>();
      collectInstances(instanceProvider, dependenciesWithoutInstances, dependencies);
    }
    if (unit.isHeader() && dependencies.contains(definition.getData())) {
      myStack.pop();
      currentState.onStack = false;
      return OrderResult.RECURSION_ERROR;
    }

    for (TCReferable referable : dependencies) {
      TCReferable tcReferable = referable.getTypecheckable();
      if (tcReferable.equals(definition.getData())) {
        if (referable.equals(tcReferable)) {
          recursion = Typechecking.Recursion.IN_BODY;
        }
      } else {
        myTypechecking.dependsOn(definition.getData(), unit.isHeader(), tcReferable);
        if (myTypechecking.getTypechecked(tcReferable) == null) {
          Concrete.ReferableDefinition dependency = myConcreteProvider.getConcrete(tcReferable);
          if (dependency instanceof Concrete.Definition) {
            updateState(currentState, new TypecheckingUnit((Concrete.Definition) dependency, myRefToHeaders));
          }
        }
      }
    }

    SCC scc = null;
    if (currentState.lowLink == currentState.index) {
      TypecheckingUnit originalUnit = unit;
      List<TypecheckingUnit> units = new ArrayList<>();
      do {
        unit = myStack.pop();
        myVertices.get(unit).onStack = false;
        units.add(unit);
      } while (!unit.equals(originalUnit));
      Collections.reverse(units);
      scc = new SCC(units);

      if (myRefToHeaders) {
        myTypechecking.sccFound(scc);
        return OrderResult.REPORTED;
      }

      if (unit.isHeader() && units.size() == 1) {
        return OrderResult.NOT_REPORTED;
      }

      if (units.size() == 1) {
        myTypechecking.unitFound(unit, recursion);
        return OrderResult.REPORTED;
      }
    }

    if (header != null) {
      myTypechecking.sccFound(new SCC(Collections.singletonList(header)));
    }
    if (scc != null) {
      myTypechecking.sccFound(scc);
    }

    return OrderResult.REPORTED;
  }
}
