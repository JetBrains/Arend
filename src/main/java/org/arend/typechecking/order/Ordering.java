package org.arend.typechecking.order;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.listener.OrderingListener;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.typecheckable.TypecheckingUnit;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;
import org.arend.typechecking.visitor.CollectDefCallsVisitor;

import java.util.*;

public class Ordering {
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
  private final Stack<TypecheckingUnit> myStack = new Stack<>();
  private final Map<TypecheckingUnit, DefState> myVertices = new HashMap<>();
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private final OrderingListener myOrderingListener;
  private final DependencyListener myDependencyListener;
  private final ReferableConverter myReferableConverter;
  private final TypecheckerState myState;
  private final PartialComparator<TCReferable> myComparator;
  private final boolean myRefToHeaders;
  private final boolean myRefToUse;

  public Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, TypecheckerState state, PartialComparator<TCReferable> comparator, boolean refToHeaders, boolean refToUse) {
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myOrderingListener = orderingListener;
    myDependencyListener = dependencyListener;
    myReferableConverter = referableConverter;
    myState = state;
    myComparator = comparator;
    myRefToHeaders = refToHeaders;
    myRefToUse = refToUse;
  }

  public Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, TypecheckerState state, PartialComparator<TCReferable> comparator) {
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myOrderingListener = orderingListener;
    myDependencyListener = dependencyListener;
    myReferableConverter = referableConverter;
    myState = state;
    myComparator = comparator;
    myRefToHeaders = false;
    myRefToUse = true;
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

  public ReferableConverter getReferableConverter() {
    return myReferableConverter;
  }

  public PartialComparator<TCReferable> getComparator() {
    return myComparator;
  }

  public void orderModules(Collection<? extends Group> modules) {
    for (Group group : modules) {
      orderModule(group);
    }
  }

  public void orderModule(Group group) {
    LocatedReferable referable = group.getReferable();
    TCReferable tcReferable = myReferableConverter.toDataLocatedReferable(referable);
    if (tcReferable == null || getTypechecked(tcReferable) == null) {
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
    if (getTypechecked(definition.getData()) != null) {
      return;
    }

    TypecheckingOrderingListener.checkCanceled();

    TypecheckingUnit typecheckingUnit = new TypecheckingUnit(definition, myRefToHeaders);
    if (!myVertices.containsKey(typecheckingUnit)) {
      // myDependencyListener.update(definition.getData());
      doOrderRecursively(typecheckingUnit);
    }
  }

  public Definition getTypechecked(TCReferable definition) {
    Definition typechecked = myState.getTypechecked(definition);
    return typechecked == null || typechecked.status().needsTypeChecking() ? null : typechecked;
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

  private OrderResult doOrderRecursively(TypecheckingUnit unit) {
    Concrete.Definition definition = unit.getDefinition();
    DefState currentState = new DefState(myIndex);
    myVertices.put(unit, currentState);
    myIndex++;
    myStack.push(unit);

    Set<TCReferable> dependencies = new LinkedHashSet<>();
    InstanceProvider instanceProvider = myInstanceProviderSet.get(definition.getData());
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myConcreteProvider, instanceProvider, dependencies);
    if (definition.enclosingClass != null) {
      visitor.addDependency(definition.enclosingClass);
    }
    if (definition instanceof Concrete.UseDefinition) {
      visitor.addDependency(((Concrete.UseDefinition) definition).getUseParent());
    }
    if (myRefToUse) {
      for (TCReferable usedDefinition : definition.getUsedDefinitions()) {
        visitor.addDependency(usedDefinition);
      }
    }

    TypecheckingOrderingListener.Recursion recursion = TypecheckingOrderingListener.Recursion.NO;
    definition.accept(visitor, null);

    for (TCReferable referable : dependencies) {
      TCReferable tcReferable = referable.getTypecheckable();
      if (tcReferable == null) {
        continue;
      }

      if (tcReferable.equals(definition.getData())) {
        if (referable.equals(tcReferable)) {
          recursion = TypecheckingOrderingListener.Recursion.IN_BODY;
        }
      } else {
        myDependencyListener.dependsOn(definition.getData(), tcReferable);
        Concrete.ReferableDefinition dependency = myConcreteProvider.getConcrete(tcReferable);
        if (dependency instanceof Concrete.Definition) {
          Definition typechecked = myState.getTypechecked(tcReferable);
          if (typechecked == null || typechecked.status() == Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING) {
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
      new DefinitionComparator(myComparator).sort(units);
      scc = new SCC(units);

      if (myRefToHeaders) {
        myOrderingListener.sccFound(scc);
        return OrderResult.REPORTED;
      }

      if (units.size() == 1) {
        myOrderingListener.definitionFound(unit, recursion);
        return OrderResult.REPORTED;
      }
    }

    if (scc != null) {
      myOrderingListener.sccFound(scc);
    }

    return OrderResult.REPORTED;
  }
}
