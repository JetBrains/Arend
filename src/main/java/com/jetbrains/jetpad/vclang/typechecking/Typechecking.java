package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.OpenCommand;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.DefinitionResolveInstanceVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceNamespaceProvider;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.Collection;
import java.util.function.Function;

public class Typechecking {
  private final InstanceNamespaceProvider myInstanceNamespaceProvider;
  private final TypecheckingDependencyListener myDependencyListener;
  private final Function<Abstract.Definition, Iterable<OpenCommand>> myOpens;

  public Typechecking(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, Function<Abstract.Definition, Iterable<OpenCommand>> opens, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener dependencyListener) {
    myInstanceNamespaceProvider = new InstanceNamespaceProvider(errorReporter);
    myDependencyListener = new TypecheckingDependencyListener(state, staticNsProvider, dynamicNsProvider, myInstanceNamespaceProvider, errorReporter, typecheckedReporter, dependencyListener);
    myOpens = opens;
  }

  public void typecheckModules(final Collection<? extends Abstract.ClassDefinition> classDefs) {
    DefinitionResolveInstanceVisitor visitor = new DefinitionResolveInstanceVisitor(myDependencyListener.getInstanceProviderProvider(), myInstanceNamespaceProvider, myOpens, myDependencyListener.getErrorReporter());
    for (Abstract.ClassDefinition classDef : classDefs) {
      visitor.visitClass(classDef, new SimpleInstanceProvider(new EmptyScope()));
    }

    Ordering ordering = new Ordering(myDependencyListener.getInstanceProviderProvider(), myDependencyListener, false);

    try {
      for (Abstract.ClassDefinition classDef : classDefs) {
        new OrderDefinitionVisitor(ordering).orderDefinition(classDef);
      }
    } catch (ComputationInterruptedException ignored) { }
  }

  public void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions) {
    Ordering ordering = new Ordering(myDependencyListener.getInstanceProviderProvider(), myDependencyListener, false);

    try {
      for (Abstract.Definition definition : definitions) {
        ordering.doOrder(definition);
      }
    } catch (ComputationInterruptedException ignored) { }
  }

  private static class OrderDefinitionVisitor extends BaseAbstractVisitor<Void, Void> {
    public final Ordering ordering;

    private OrderDefinitionVisitor(Ordering ordering) {
      this.ordering = ordering;
    }

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, Void params) {
      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        orderDefinition(definition);
      }
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, Void params) {
      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        orderDefinition(definition);
      }
      for (Abstract.Definition definition : def.getInstanceDefinitions()) {
        orderDefinition(definition);
      }
      return null;
    }

    public void orderDefinition(Abstract.Definition definition) {
      ordering.doOrder(definition);
      definition.accept(this, null);
    }
  }
}
