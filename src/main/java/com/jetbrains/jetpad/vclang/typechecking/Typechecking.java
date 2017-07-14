package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.OpenCommand;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.DefinitionResolveInstanceVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.ClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceScopeProvider;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.Collection;
import java.util.function.Function;

public class Typechecking {
  private final InstanceScopeProvider myScopeProvider;
  private final TypecheckingDependencyListener myDependencyListener;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final Function<Abstract.Definition, Iterable<OpenCommand>> myOpens;
  private final ErrorReporter myErrorReporter;

  public Typechecking(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, Function<Abstract.Definition, Iterable<OpenCommand>> opens, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener dependencyListener) {
    myOpens = opens;
    myDependencyListener = new TypecheckingDependencyListener(state, staticNsProvider, dynamicNsProvider, errorReporter, typecheckedReporter, dependencyListener);
    myScopeProvider = new InstanceScopeProvider(errorReporter);
    myStaticNsProvider = staticNsProvider;
    myErrorReporter = errorReporter;
  }

  public void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    for (Abstract.Definition definition : definitions) {
      definition.accept(new DefinitionResolveInstanceVisitor(myScopeProvider, instanceProvider, myOpens, myErrorReporter), getDefinitionScope(definition));
    }
    typecheckDefinitions(definitions, instanceProvider);
  }

  public void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions, Scope scope) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    DefinitionResolveInstanceVisitor visitor = new DefinitionResolveInstanceVisitor(myScopeProvider, instanceProvider, myOpens, myErrorReporter);
    for (Abstract.Definition definition : definitions) {
      definition.accept(visitor, scope);
    }
    typecheckDefinitions(definitions, instanceProvider);
  }

  public void typecheckModules(final Collection<? extends Abstract.ClassDefinition> classDefs) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    for (Abstract.ClassDefinition classDef : classDefs) {
      classDef.accept(new DefinitionResolveInstanceVisitor(myScopeProvider, instanceProvider, myOpens, myErrorReporter), new EmptyScope());
    }

    myDependencyListener.setInstanceProvider(instanceProvider);
    Ordering ordering = new Ordering(instanceProvider, myDependencyListener, false);

    try {
      for (Abstract.ClassDefinition classDef : classDefs) {
        new OrderDefinitionVisitor(ordering).orderDefinition(classDef);
      }
    } catch (ComputationInterruptedException ignored) { }
  }


  private void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions, ClassViewInstanceProvider instanceProvider) {
    myDependencyListener.setInstanceProvider(instanceProvider);
    Ordering ordering = new Ordering(instanceProvider, myDependencyListener, false);

    try {
      for (Abstract.Definition definition : definitions) {
        ordering.doOrder(definition);
      }
    } catch (ComputationInterruptedException ignored) { }
  }

  private Scope getDefinitionScope(Abstract.Definition definition) {
    if (definition == null) {
      return new EmptyScope();
    }

    return definition.accept(new BaseAbstractVisitor<Scope, Scope>() {
      @Override
      public Scope visitFunction(Abstract.FunctionDefinition def, Scope parentScope) {
        return new FunctionScope(parentScope, new NamespaceScope(myStaticNsProvider.forDefinition(def)));
      }

      @Override
      public Scope visitData(Abstract.DataDefinition def, Scope parentScope) {
        return new DataScope(parentScope, new NamespaceScope(myStaticNsProvider.forDefinition(def)));
      }

      @Override
      public Scope visitClass(Abstract.ClassDefinition def, Scope parentScope) {
        return new StaticClassScope(parentScope, new NamespaceScope(myStaticNsProvider.forDefinition(def)));
      }
    }, getDefinitionScope(definition.getParentDefinition()));
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
