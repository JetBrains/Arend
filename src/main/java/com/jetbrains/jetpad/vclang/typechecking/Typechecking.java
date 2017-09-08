package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.resolving.GroupResolver;
import com.jetbrains.jetpad.vclang.naming.resolving.NamespaceProviders;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.TypecheckableProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.GroupInstanceResolver;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceNamespaceProvider;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.Collection;

public class Typechecking {
  private final InstanceNamespaceProvider myInstanceNamespaceProvider; // TODO[classes]
  private final TypecheckingDependencyListener myDependencyListener;
  private final NameResolver myNameResolver;

  public Typechecking(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, TypecheckableProvider typecheckableProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener dependencyListener) {
    myInstanceNamespaceProvider = new InstanceNamespaceProvider(errorReporter);
    myDependencyListener = new TypecheckingDependencyListener(state, staticNsProvider, dynamicNsProvider, typecheckableProvider, errorReporter, typecheckedReporter, dependencyListener);
    myNameResolver = new NameResolver(new NamespaceProviders(null, staticNsProvider, dynamicNsProvider));
  }

  public void typecheckModules(final Collection<? extends Group> modules) {
    GroupResolver resolver = new GroupInstanceResolver(myNameResolver, myDependencyListener.errorReporter, myDependencyListener.instanceProviderSet);
    Scope emptyScope = new EmptyScope();
    for (Group group : modules) {
      resolver.resolveGroup(group, emptyScope);
    }
    Ordering ordering = new Ordering(myDependencyListener.instanceProviderSet, myDependencyListener.typecheckableProvider, myDependencyListener, false);

    try {
      for (Group group : modules) {
        orderGroup(group, ordering);
      }
    } catch (ComputationInterruptedException ignored) { }
  }

  public void typecheckDefinitions(final Collection<? extends Concrete.Definition> definitions) {
    Ordering ordering = new Ordering(myDependencyListener.instanceProviderSet, myDependencyListener.typecheckableProvider, myDependencyListener, false);

    try {
      for (Concrete.Definition definition : definitions) {
        ordering.doOrder(definition);
      }
    } catch (ComputationInterruptedException ignored) { }
  }

  private void orderGroup(Group group, Ordering ordering) {
    Concrete.ReferableDefinition def = myDependencyListener.typecheckableProvider.getTypecheckable(group.getReferable());
    if (def instanceof Concrete.Definition) {
      ordering.doOrder((Concrete.Definition) def);
    }
    for (Group subgroup : group.getSubgroups()) {
      orderGroup(subgroup, ordering);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      orderGroup(subgroup, ordering);
    }
  }
}
