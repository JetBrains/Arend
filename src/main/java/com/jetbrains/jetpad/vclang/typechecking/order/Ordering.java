package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.order.listener.TypecheckingOrderingListener;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.order.listener.OrderingListener;
import com.jetbrains.jetpad.vclang.typechecking.order.dependency.DefinitionGetDependenciesVisitor;
import com.jetbrains.jetpad.vclang.typechecking.order.dependency.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;
import com.jetbrains.jetpad.vclang.typechecking.instance.provider.InstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.instance.provider.InstanceProviderSet;

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
  private final OrderingListener myOrderingListener;
  private final DependencyListener myDependencyListener;
  private final ReferableConverter myReferableConverter;
  private final TypecheckerState myState;
  private final boolean myRefToHeaders;

  public Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, TypecheckerState state, boolean refToHeaders) {
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myOrderingListener = orderingListener;
    myDependencyListener = dependencyListener;
    myReferableConverter = referableConverter;
    myState = state;
    myRefToHeaders = refToHeaders;
  }

  public Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, TypecheckerState state) {
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myOrderingListener = orderingListener;
    myDependencyListener = dependencyListener;
    myReferableConverter = referableConverter;
    myState = state;
    myRefToHeaders = false;
  }

  public TypecheckerState getTypecheckerState() {
    return myState;
  }

  public DependencyListener getDependencyListener() {
    return myDependencyListener;
  }

  public InstanceProviderSet getInstanceProviderSet() {
    return myInstanceProviderSet;
  }

  public ConcreteProvider getConcreteProvider() {
    return myConcreteProvider;
  }

  public void orderModules(Collection<? extends Group> modules) {
    for (Group group : modules) {
      orderModule(group);
    }
  }

  public void orderModule(Group group) {
    LocatedReferable referable = group.getReferable();
    TCReferable tcReferable = myReferableConverter.toDataLocatedReferable(referable);
    Definition typechecked = tcReferable == null ? null : getTypechecked(tcReferable);
    if (typechecked == null) {
      Concrete.ReferableDefinition def = myConcreteProvider.getConcrete(referable);
      if (def instanceof Concrete.Definition) {
        orderDefinition((Concrete.Definition) def);
      }
    }

    for (Group subgroup : group.getSubgroups()) {
      orderModule(subgroup);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      orderModule(subgroup);
    }
  }

  public void orderDefinition(Concrete.Definition definition) {
    TypecheckingUnit typecheckingUnit = new TypecheckingUnit(definition, myRefToHeaders);
    if (!myVertices.containsKey(typecheckingUnit)) {
      doOrderRecursively(typecheckingUnit);
    }
  }

  public Definition getTypechecked(TCReferable definition) {
    Definition typechecked = myState.getTypechecked(definition);
    if (typechecked == null || typechecked.status().needsTypeChecking()) {
      return null;
    } else {
      return typechecked;
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

  private void collectInstances(InstanceProvider instanceProvider, Deque<TCReferable> referables, Set<TCReferable> result) {
    while (!referables.isEmpty()) {
      TCReferable referable = referables.pop();
      if (!result.add(referable)) {
        continue;
      }

      Concrete.ReferableDefinition definition = myConcreteProvider.getConcrete(referable);
      if (definition instanceof Concrete.ClassFieldSynonym) {
        Referable classRef = ((Concrete.ClassFieldSynonym) definition).getRelatedDefinition().getUnderlyingClass().getReferent();
        if (classRef instanceof ClassReferable) {
          for (Concrete.Instance instance : instanceProvider.getInstances((ClassReferable) classRef)) {
            referables.push(instance.getData());
          }
        }
      } else if (definition != null) {
        Collection<? extends Concrete.Parameter> parameters = Concrete.getParameters(definition);
        if (parameters != null) {
          for (Concrete.Parameter parameter : parameters) {
            ClassReferable classDef = Concrete.getUnderlyingClassDef(((Concrete.TypeParameter) parameter).getType());
            if (classDef != null) {
              for (Concrete.Instance instance : instanceProvider.getInstances(classDef)) {
                referables.push(instance.getData());
              }
            }
          }
        }
      }
    }
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
        myOrderingListener.unitFound(unit, TypecheckingOrderingListener.Recursion.IN_HEADER);
        return OrderResult.REPORTED;
      }

      if (result == OrderResult.REPORTED) {
        header = null;
      }
    }

    Set<TCReferable> dependencies = new LinkedHashSet<>();
    if (definition.enclosingClass != null) {
      dependencies.add(definition.enclosingClass);
    }

    TypecheckingOrderingListener.Recursion recursion = TypecheckingOrderingListener.Recursion.NO;
    definition.accept(new DefinitionGetDependenciesVisitor(dependencies), unit.isHeader());
    InstanceProvider instanceProvider = myInstanceProviderSet.get(definition.getData());
    if (instanceProvider != null) {
      Deque<TCReferable> deque = new ArrayDeque<>(dependencies);
      dependencies = new LinkedHashSet<>();
      collectInstances(instanceProvider, deque, dependencies);
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
          recursion = TypecheckingOrderingListener.Recursion.IN_BODY;
        }
      } else {
        myDependencyListener.dependsOn(definition.getData(), unit.isHeader(), tcReferable);
        Concrete.ReferableDefinition dependency = myConcreteProvider.getConcrete(tcReferable);
        if (dependency instanceof Concrete.Definition && getTypechecked(tcReferable) == null) {
          updateState(currentState, new TypecheckingUnit((Concrete.Definition) dependency, myRefToHeaders));
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
        myOrderingListener.sccFound(scc);
        return OrderResult.REPORTED;
      }

      if (unit.isHeader() && units.size() == 1) {
        return OrderResult.NOT_REPORTED;
      }

      if (units.size() == 1) {
        myOrderingListener.unitFound(unit, recursion);
        return OrderResult.REPORTED;
      }
    }

    if (header != null) {
      myOrderingListener.sccFound(new SCC(Collections.singletonList(header)));
    }
    if (scc != null) {
      myOrderingListener.sccFound(scc);
    }

    return OrderResult.REPORTED;
  }
}
