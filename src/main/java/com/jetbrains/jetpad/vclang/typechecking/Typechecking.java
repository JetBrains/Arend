package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.OpenCommand;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.DefinitionResolveInstanceVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceNamespaceProvider;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.Collection;
import java.util.function.Function;

public class Typechecking<T> {
  private final InstanceNamespaceProvider myInstanceNamespaceProvider;
  private final TypecheckingDependencyListener<T> myDependencyListener;
  private final Function<Abstract.Definition, Iterable<OpenCommand>> myOpens;

  public Typechecking(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, Function<Abstract.Definition, Iterable<OpenCommand>> opens, TypecheckableProvider<T> typecheckableProvider, ErrorReporter<T> errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener<T> dependencyListener) {
    myInstanceNamespaceProvider = new InstanceNamespaceProvider(errorReporter);
    myDependencyListener = new TypecheckingDependencyListener<>(state, staticNsProvider, dynamicNsProvider, myInstanceNamespaceProvider, typecheckableProvider, errorReporter, typecheckedReporter, dependencyListener);
    myOpens = opens;
  }

  public void typecheckModules(final Collection<? extends Concrete.ClassDefinition<T>> classDefs) {
    DefinitionResolveInstanceVisitor visitor = new DefinitionResolveInstanceVisitor(myDependencyListener.instanceProviderSet, myInstanceNamespaceProvider, myOpens, myDependencyListener.errorReporter);
    for (Abstract.ClassDefinition classDef : classDefs) {
      visitor.visitClass(classDef, new SimpleInstanceProvider(new EmptyScope()));
    }

    Ordering<T> ordering = new Ordering<>(myDependencyListener.instanceProviderSet, myDependencyListener.typecheckableProvider, myDependencyListener, false);

    try {
      for (Concrete.ClassDefinition<T> classDef : classDefs) {
        new OrderDefinitionVisitor<>(ordering).orderDefinition(classDef);
      }
    } catch (ComputationInterruptedException ignored) { }
  }

  public void typecheckDefinitions(final Collection<? extends Concrete.Definition<T>> definitions) {
    Ordering<T> ordering = new Ordering<>(myDependencyListener.instanceProviderSet, myDependencyListener.typecheckableProvider, myDependencyListener, false);

    try {
      for (Concrete.Definition<T> definition : definitions) {
        ordering.doOrder(definition);
      }
    } catch (ComputationInterruptedException ignored) { }
  }

  private static class OrderDefinitionVisitor<T> extends BaseAbstractVisitor<Void, Void> { // TODO[abstract]
    public final Ordering<T> ordering;

    private OrderDefinitionVisitor(Ordering<T> ordering) {
      this.ordering = ordering;
    }

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, Void params) {
      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        orderDefinition((Concrete.Definition<T>) definition);
      }
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, Void params) {
      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        orderDefinition((Concrete.Definition<T>) definition);
      }
      for (Abstract.Definition definition : def.getInstanceDefinitions()) {
        orderDefinition((Concrete.Definition<T>) definition);
      }
      return null;
    }

    public void orderDefinition(Concrete.Definition<T> definition) {
      ordering.doOrder(definition);
      definition.accept(this, null);
    }
  }
}
