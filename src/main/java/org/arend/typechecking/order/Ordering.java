package org.arend.typechecking.order;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.listener.OrderingListener;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.visitor.CollectDefCallsVisitor;

import java.util.*;
import java.util.function.Consumer;

public class Ordering extends BellmanFord<Concrete.Definition> {
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private final OrderingListener myOrderingListener;
  private final DependencyListener myDependencyListener;
  private final ReferableConverter myReferableConverter;
  private final TypecheckerState myState;
  private final PartialComparator<TCReferable> myComparator;
  private final boolean myWithBodies;
  private final boolean myWithUse;

  private Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, TypecheckerState state, PartialComparator<TCReferable> comparator, boolean withBodies, boolean withUse) {
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myOrderingListener = orderingListener;
    myDependencyListener = dependencyListener;
    myReferableConverter = referableConverter;
    myState = state;
    myComparator = comparator;
    myWithBodies = withBodies;
    myWithUse = withUse;
  }

  private Ordering(Ordering ordering, boolean withBodies, boolean withUse) {
    this(ordering.myInstanceProviderSet, ordering.myConcreteProvider, ordering.myOrderingListener, ordering.myDependencyListener, ordering.myReferableConverter, ordering.myState, ordering.myComparator, withBodies, withUse);
  }

  public Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, TypecheckerState state, PartialComparator<TCReferable> comparator) {
    this(instanceProviderSet, concreteProvider, orderingListener, dependencyListener, referableConverter, state, comparator, true, true);
  }

  public TypecheckerState getTypecheckerState() {
    return myState;
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
        order((Concrete.Definition) def);
      }
    }

    for (Group subgroup : group.getSubgroups()) {
      orderModule(subgroup);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      orderModule(subgroup);
    }
  }

  @Override
  public void order(Concrete.Definition definition) {
    if (getTypechecked(definition.getData()) == null) {
      TypecheckingOrderingListener.checkCanceled();
      super.order(definition);
    }
  }

  public Definition getTypechecked(TCReferable definition) {
    Definition typechecked = myState.getTypechecked(definition);
    return typechecked == null || typechecked.status().needsTypeChecking() ? null : typechecked;
  }

  @Override
  protected boolean forDependencies(Concrete.Definition definition, Consumer<Concrete.Definition> consumer) {
    Set<TCReferable> dependencies = new LinkedHashSet<>();
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myConcreteProvider, myInstanceProviderSet.get(definition.getData()), dependencies, myWithBodies);
    if (definition.enclosingClass != null) {
      visitor.addDependency(definition.enclosingClass);
    }
    if (definition instanceof Concrete.UseDefinition) {
      visitor.addDependency(((Concrete.UseDefinition) definition).getUseParent());
    }
    if (myWithUse) {
      for (TCReferable usedDefinition : definition.getUsedDefinitions()) {
        visitor.addDependency(usedDefinition);
      }
    }
    definition.accept(visitor, null);

    boolean withLoops = false;
    for (TCReferable referable : dependencies) {
      TCReferable tcReferable = referable.getTypecheckable();
      if (tcReferable == null) {
        continue;
      }

      if (tcReferable.equals(definition.getData())) {
        if (referable.equals(tcReferable)) {
          withLoops = true;
        }
      } else {
        myDependencyListener.dependsOn(definition.getData(), tcReferable);
        Concrete.ReferableDefinition dependency = myConcreteProvider.getConcrete(tcReferable);
        if (dependency instanceof Concrete.Definition) {
          Definition typechecked = myState.getTypechecked(tcReferable);
          if (typechecked == null || typechecked.status() == Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING) {
            consumer.accept((Concrete.Definition) dependency);
          }
        }
      }
    }
    return withLoops;
  }

  @Override
  protected void unitFound(Concrete.Definition unit, boolean withLoops) {
    if (myWithBodies) {
      myOrderingListener.unitFound(unit, withLoops);
    } else {
      if (withLoops) {
        myOrderingListener.cycleFound(Collections.singletonList(unit));
      } else {
        myOrderingListener.headerFound(unit);
      }
    }
  }

  @Override
  protected void sccFound(List<Concrete.Definition> scc) {
    if (!myWithBodies) {
      myOrderingListener.cycleFound(scc);
      return;
    }
    if (scc.isEmpty()) {
      return;
    }
    if (scc.size() == 1) {
      myOrderingListener.unitFound(scc.get(0), true);
      return;
    }

    boolean hasUse = false;
    for (Concrete.Definition definition : scc) {
      if (definition instanceof Concrete.UseDefinition) {
        hasUse = true;
        break;
      }
    }

    if (hasUse) {
      if (!myWithUse) {
        myOrderingListener.cycleFound(scc);
        return;
      }

      Ordering ordering = new Ordering(this, true, false);
      for (Concrete.Definition definition : scc) {
        ordering.order(definition);
      }
      return;
    }

    for (Concrete.Definition definition : scc) {
      if (definition instanceof Concrete.ClassDefinition) {
        myOrderingListener.cycleFound(scc);
        return;
      }
    }

    Ordering ordering = new Ordering(this, false, false);
    for (Concrete.Definition definition : scc) {
      ordering.order(definition);
    }

    new DefinitionComparator(myComparator).sort(scc);
    myOrderingListener.bodiesFound(scc);
  }
}
