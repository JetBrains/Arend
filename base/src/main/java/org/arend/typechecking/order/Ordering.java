package org.arend.typechecking.order;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.typechecking.computation.ComputationRunner;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.listener.OrderingListener;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.visitor.CollectDefCallsVisitor;

import java.util.*;
import java.util.function.Consumer;

public class Ordering extends BellmanFord<Concrete.ResolvableDefinition> {
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private OrderingListener myOrderingListener;
  private final DependencyListener myDependencyListener;
  private final ReferableConverter myReferableConverter;
  private final PartialComparator<TCDefReferable> myComparator;
  private final Set<TCReferable> myAllowedDependencies;
  private final Stage myStage;

  private enum Stage { EVERYTHING, WITHOUT_INSTANCES, WITHOUT_USE, WITHOUT_BODIES }

  private Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, PartialComparator<TCDefReferable> comparator, Set<TCReferable> allowedDependencies, Stage stage) {
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myOrderingListener = orderingListener;
    myDependencyListener = dependencyListener;
    myReferableConverter = referableConverter;
    myComparator = comparator;
    myAllowedDependencies = allowedDependencies;
    myStage = stage;
  }

  private Ordering(Ordering ordering, Set<TCReferable> allowedDependencies, Stage stage) {
    this(ordering.myInstanceProviderSet, ordering.myConcreteProvider, ordering.myOrderingListener, ordering.myDependencyListener, ordering.myReferableConverter, ordering.myComparator, allowedDependencies, stage);
  }

  public Ordering(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, OrderingListener orderingListener, DependencyListener dependencyListener, ReferableConverter referableConverter, PartialComparator<TCDefReferable> comparator) {
    this(instanceProviderSet, concreteProvider, orderingListener, dependencyListener, referableConverter, comparator, null, Stage.EVERYTHING);
  }

  public ConcreteProvider getConcreteProvider() {
    return myConcreteProvider;
  }

  public ReferableConverter getReferableConverter() {
    return myReferableConverter;
  }

  public OrderingListener getListener() {
    return myOrderingListener;
  }

  public void setListener(OrderingListener listener) {
    myOrderingListener = listener;
  }

  public void orderModules(Collection<? extends Group> modules) {
    for (Group group : modules) {
      orderModule(group);
    }
  }

  public void orderModule(Group group) {
    LocatedReferable referable = group.getReferable();
    TCReferable tcReferable = myReferableConverter.toDataLocatedReferable(referable);
    if (tcReferable instanceof TCDefReferable && getTypechecked(tcReferable) == null) {
      var def = myConcreteProvider.getConcrete(referable);
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
  public void order(Concrete.ResolvableDefinition definition) {
    if (definition.getStage() != Concrete.Stage.TYPECHECKED && getTypechecked(definition.getData()) == null) {
      ComputationRunner.checkCanceled();
      super.order(definition);
    }
  }

  public Definition getTypechecked(TCReferable definition) {
    Definition typechecked = definition instanceof TCDefReferable ? ((TCDefReferable) definition).getTypechecked() : null;
    return typechecked == null || typechecked.status().needsTypeChecking() ? null : typechecked;
  }

  @Override
  protected boolean forDependencies(Concrete.ResolvableDefinition definition, Consumer<Concrete.ResolvableDefinition> consumer) {
    Set<TCReferable> dependencies = new LinkedHashSet<>();
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(dependencies, myStage.ordinal() < Stage.WITHOUT_BODIES.ordinal());
    if (myStage.ordinal() < Stage.WITHOUT_USE.ordinal()) {
      if (myStage.ordinal() < Stage.WITHOUT_INSTANCES.ordinal()) {
        InstanceProvider instanceProvider = myInstanceProviderSet.get(definition.getData());
        if (instanceProvider != null) {
          instanceProvider.findInstance(instance -> {
            visitor.addDependency(instance);
            return false;
          });
        }
      }
      for (TCReferable usedDefinition : definition.getUsedDefinitions()) {
        visitor.addDependency(usedDefinition);
      }
    }

    if (definition.getEnclosingClass() != null) {
      visitor.addDependency(definition.getEnclosingClass());
    }
    if (definition instanceof Concrete.CoClauseFunctionDefinition) {
      Referable ref = ((Concrete.CoClauseFunctionDefinition) definition).getImplementedField();
      if (ref instanceof TCReferable) {
        visitor.addDependency((TCReferable) ref);
      }
    }
    if (definition instanceof Concrete.UseDefinition) {
      visitor.addDependency(((Concrete.UseDefinition) definition).getUseParent());
    }
    definition.accept(visitor, null);

    boolean withLoops = false;
    for (TCReferable referable : dependencies) {
      TCReferable tcReferable = referable.getTypecheckable();
      if (myAllowedDependencies != null && !myAllowedDependencies.contains(tcReferable)) {
        continue;
      }

      if (tcReferable.equals(definition.getData())) {
        if (referable.equals(tcReferable)) {
          withLoops = true;
        }
      } else {
        myDependencyListener.dependsOn(definition.getData(), tcReferable);
        if (!tcReferable.isTypechecked()) {
          var dependency = myConcreteProvider.getConcrete(tcReferable);
          if (dependency instanceof Concrete.ResolvableDefinition && dependency.getStage() != Concrete.Stage.TYPECHECKED) {
            consumer.accept((Concrete.ResolvableDefinition) dependency);
          }
        }
      }
    }
    return withLoops;
  }

  @Override
  protected void unitFound(Concrete.ResolvableDefinition unit, boolean withLoops) {
    if (myStage.ordinal() < Stage.WITHOUT_BODIES.ordinal()) {
      myOrderingListener.unitFound(unit, withLoops);
    } else {
      if (withLoops) {
        myOrderingListener.cycleFound(Collections.singletonList(unit));
      } else if (unit instanceof Concrete.Definition) {
        myOrderingListener.headerFound((Concrete.Definition) unit);
      }
    }
  }

  @Override
  protected void sccFound(List<Concrete.ResolvableDefinition> scc) {
    if (myStage.ordinal() >= Stage.WITHOUT_BODIES.ordinal()) {
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
    boolean hasInstances = false;
    for (Concrete.ResolvableDefinition definition : scc) {
      if (definition instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) definition).getKind() == FunctionKind.INSTANCE) {
        if (myStage.ordinal() >= Stage.WITHOUT_INSTANCES.ordinal()) {
          myOrderingListener.cycleFound(scc);
          return;
        }
        hasInstances = true;
        break;
      }
      if (definition instanceof Concrete.UseDefinition) {
        if (myStage.ordinal() >= Stage.WITHOUT_USE.ordinal()) {
          myOrderingListener.cycleFound(scc);
          return;
        }
        hasUse = true;
      }
      if (!(definition instanceof Concrete.Definition)) {
        myOrderingListener.cycleFound(scc);
        return;
      }
    }

    Set<TCReferable> dependencies = new HashSet<>();
    for (Concrete.ResolvableDefinition definition : scc) {
      dependencies.add(definition.getData());
    }

    if (hasInstances) {
      Ordering ordering = new Ordering(this, dependencies, Stage.WITHOUT_INSTANCES);
      for (Concrete.ResolvableDefinition definition : scc) {
        ordering.order(definition);
      }
      return;
    }

    if (hasUse) {
      Ordering ordering = new Ordering(this, dependencies, Stage.WITHOUT_USE);
      for (Concrete.ResolvableDefinition definition : scc) {
        ordering.order(definition);
      }

      List<Concrete.UseDefinition> useDefinitions = new ArrayList<>();
      for (Concrete.ResolvableDefinition definition : scc) {
        if (definition instanceof Concrete.UseDefinition) {
          useDefinitions.add((Concrete.UseDefinition) definition);
        }
      }
      myOrderingListener.useFound(useDefinitions);
      return;
    }

    for (Concrete.ResolvableDefinition definition : scc) {
      if (definition instanceof Concrete.ClassDefinition) {
        myOrderingListener.cycleFound(scc);
        return;
      }
    }

    Ordering ordering = new Ordering(this, dependencies, Stage.WITHOUT_BODIES);
    for (Concrete.ResolvableDefinition definition : scc) {
      ordering.order(definition);
    }

    List<Concrete.Definition> defs = new ArrayList<>(scc.size());
    for (Concrete.ResolvableDefinition def : scc) {
      defs.add((Concrete.Definition) def);
    }
    new DefinitionComparator(myComparator).sort(defs);
    myOrderingListener.bodiesFound(defs);
  }
}
