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
  private final Deque<Concrete.Definition> myDeferredDefinitions = new ArrayDeque<>();
  private final boolean myRefToHeaders;

  public Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, TypecheckerState state, PartialComparator<TCReferable> comparator, boolean refToHeaders) {
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myOrderingListener = orderingListener;
    myDependencyListener = dependencyListener;
    myReferableConverter = referableConverter;
    myState = state;
    myComparator = comparator;
    myRefToHeaders = refToHeaders;
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
    while (!myDeferredDefinitions.isEmpty()) {
      doOrderRecursively(new TypecheckingUnit(myDeferredDefinitions.pop(), false));
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
    InstanceProvider instanceProvider = myInstanceProviderSet.get(definition.getData());
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myConcreteProvider, instanceProvider, dependencies);
    if (definition.enclosingClass != null) {
      visitor.addDependency(definition.enclosingClass);
    }

    TypecheckingOrderingListener.Recursion recursion = TypecheckingOrderingListener.Recursion.NO;
    definition.accept(visitor, unit.isHeader());

    if (unit.isHeader() && dependencies.contains(definition.getData())) {
      myStack.pop();
      currentState.onStack = false;
      return OrderResult.RECURSION_ERROR;
    }

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
        myDependencyListener.dependsOn(definition.getData(), unit.isHeader(), tcReferable);
        Concrete.ReferableDefinition dependency = myConcreteProvider.getConcrete(tcReferable);
        if (dependency instanceof Concrete.Definition) {
          Definition typechecked = myState.getTypechecked(tcReferable);
          if (typechecked == null || typechecked.status() == Definition.TypeCheckingStatus.HEADER_HAS_ERRORS) {
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

      // This can happen only when the definition is \\use \\coerce
      if (units.size() == 2 && units.get(0).getDefinition().getData() == units.get(1).getDefinition().getData() && units.get(0).isHeader() != units.get(1).isHeader()) {
        units.remove(units.get(0).isHeader() ? 1 : 0);
      }

      Collections.reverse(units);
      new TypecheckingUnitComparator(myComparator).sort(units);
      scc = new SCC(units);

      if (myRefToHeaders) {
        myOrderingListener.sccFound(scc);
        return OrderResult.REPORTED;
      }

      if (unit.isHeader() && units.size() == 1) {
        if (unit.getDefinition() instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) unit.getDefinition()).getKind() == Concrete.FunctionDefinition.Kind.LEVEL) {
          myOrderingListener.unitFound(unit, recursion);
          return OrderResult.REPORTED;
        }
        return OrderResult.NOT_REPORTED;
      }

      if (units.size() == 1) {
        myOrderingListener.unitFound(unit, recursion);
        doOrderUsedDefinitions(currentState, unit.getDefinition());
        return OrderResult.REPORTED;
      }
    }

    if (header != null) {
      myOrderingListener.sccFound(new SCC(Collections.singletonList(header)));
    }
    if (scc != null) {
      myOrderingListener.sccFound(scc);
      for (TypecheckingUnit sccUnit : scc.getUnits()) {
        doOrderUsedDefinitions(currentState, sccUnit.getDefinition());
      }
    }

    return OrderResult.REPORTED;
  }

  private void doOrderUsedDefinitions(DefState currentState, Concrete.Definition definition) {
    for (TCReferable usedDefinition : definition.getUsedDefinitions()) {
      Concrete.FunctionDefinition def = myConcreteProvider.getConcreteFunction(usedDefinition);
      if (def != null) {
        Concrete.FunctionDefinition.Kind kind = def.getKind();
        if (kind.isUse()) {
          Definition typechecked = myState.getTypechecked(def.getData());
          if (typechecked == null || typechecked.status() == Definition.TypeCheckingStatus.HEADER_HAS_ERRORS) {
            updateState(currentState, new TypecheckingUnit(def, kind == Concrete.FunctionDefinition.Kind.LEVEL));
            if (kind == Concrete.FunctionDefinition.Kind.LEVEL) {
              myDeferredDefinitions.add(def);
            }
          }
        }
      }
    }
  }
}
