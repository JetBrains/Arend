package org.arend.typechecking.order;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.dependency.DefinitionGetDependenciesVisitor;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.listener.OrderingListener;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.typecheckable.TypecheckingUnit;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

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

  public ReferableConverter getReferableConverter() {
    return myReferableConverter;
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
      if (referable.getUnderlyingReference() != null) {
        continue;
      }

      Concrete.ReferableDefinition definition = myConcreteProvider.getConcrete(referable);
      if (definition instanceof Concrete.ClassField) {
        ClassReferable classRef = ((Concrete.ClassField) definition).getRelatedDefinition().getData();
        for (Concrete.Instance instance : instanceProvider.getInstances()) {
          Referable ref = instance.getReferenceInType();
          if (ref instanceof ClassReferable && ((ClassReferable) ref).isSubClassOf(classRef)) {
            referables.push(instance.getData());
          }
        }
      } else if (definition != null) {
        Collection<? extends Concrete.Parameter> parameters = Concrete.getParameters(definition);
        if (parameters != null) {
          for (Concrete.Parameter parameter : parameters) {
            TCClassReferable classRef = ((Concrete.TypeParameter) parameter).getType().getUnderlyingClassReferable(true);
            if (classRef != null) {
              for (Concrete.Instance instance : instanceProvider.getInstances()) {
                Referable ref = instance.getReferenceInType();
                if (ref instanceof ClassReferable && ((ClassReferable) ref).isSubClassOf(classRef)) {
                  referables.push(instance.getData());
                }
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
    if (definition instanceof Concrete.ClassDefinition) {
      new ClassFieldImplScope(((Concrete.ClassDefinition) definition).getData(), false).find(ref -> {
        if (ref instanceof TCReferable) {
          dependencies.remove(ref);
        }
        return false;
      });
    }

    InstanceProvider instanceProvider = myInstanceProviderSet.get(definition.getData());
    if (instanceProvider != null) {
      Deque<TCReferable> deque = new ArrayDeque<>(dependencies);
      dependencies.clear();
      collectInstances(instanceProvider, deque, dependencies);
    }
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
        if (referable.getUnderlyingReference() == null) {
          Concrete.ReferableDefinition dependency = myConcreteProvider.getConcrete(tcReferable);
          if (dependency instanceof Concrete.Definition && getTypechecked(tcReferable) == null) {
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
        myOrderingListener.sccFound(scc);
        return OrderResult.REPORTED;
      }

      if (unit.isHeader() && units.size() == 1) {
        return OrderResult.NOT_REPORTED;
      }

      if (units.size() == 1) {
        myOrderingListener.unitFound(unit, recursion);
        doOrderCoercingFunctions(unit.getDefinition());
        return OrderResult.REPORTED;
      }
    }

    if (header != null) {
      myOrderingListener.sccFound(new SCC(Collections.singletonList(header)));
    }
    if (scc != null) {
      myOrderingListener.sccFound(scc);
      for (TypecheckingUnit unit1 : scc.getUnits()) {
        doOrderCoercingFunctions(unit1.getDefinition());
      }
    }

    return OrderResult.REPORTED;
  }

  private void doOrderCoercingFunctions(Concrete.Definition definition) {
    if (myRefToHeaders) {
      return;
    }

    List<TCReferable> coercingFunctions = Collections.emptyList();
    if (definition instanceof Concrete.DataDefinition) {
      coercingFunctions = ((Concrete.DataDefinition) definition).getCoercingFunctions();
    } else if (definition instanceof Concrete.ClassDefinition) {
      coercingFunctions = ((Concrete.ClassDefinition) definition).getCoercingFunctions();
    }

    for (TCReferable coercingFunction : coercingFunctions) {
      myDependencyListener.dependsOn(definition.getData(), true, coercingFunction);
      Concrete.ReferableDefinition def = myConcreteProvider.getConcrete(coercingFunction);
      if (def instanceof Concrete.Definition) {
        orderDefinition((Concrete.Definition) def);
      }
    }
  }
}
