package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.DefinitionResolveInstanceVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.ClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceScopeProvider;

import java.util.Collection;

public class Typechecking {
  private final InstanceScopeProvider myScopeProvider;
  private final TypecheckingDependencyListener myDependencyListener;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final ErrorReporter myErrorReporter;

  public Typechecking(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener dependencyListener) {
    myDependencyListener = new TypecheckingDependencyListener(state, staticNsProvider, dynamicNsProvider, errorReporter, typecheckedReporter, dependencyListener);
    myScopeProvider = new InstanceScopeProvider(errorReporter);
    myStaticNsProvider = staticNsProvider;
    myErrorReporter = errorReporter;
  }

  public void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    for (Abstract.Definition definition : definitions) {
      definition.accept(new DefinitionResolveInstanceVisitor(myScopeProvider, instanceProvider, myErrorReporter), getDefinitionScope(definition));
    }
    typecheckDefinitions(definitions, instanceProvider);
  }

  public void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions, Scope scope) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    DefinitionResolveInstanceVisitor visitor = new DefinitionResolveInstanceVisitor(myScopeProvider, instanceProvider, myErrorReporter);
    for (Abstract.Definition definition : definitions) {
      definition.accept(visitor, scope);
    }
    typecheckDefinitions(definitions, instanceProvider);
  }

  public void typecheckModules(final Collection<? extends Abstract.ClassDefinition> classDefs) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    for (Abstract.ClassDefinition classDef : classDefs) {
      classDef.accept(new DefinitionResolveInstanceVisitor(myScopeProvider, instanceProvider, myErrorReporter), new EmptyScope());
    }

    myDependencyListener.setInstanceProvider(instanceProvider);
    Ordering ordering = new Ordering(instanceProvider, myDependencyListener, false);
    for (Abstract.ClassDefinition classDef : classDefs) {
      new OrderDefinitionVisitor(ordering).orderDefinition(classDef);
    }
  }


  private void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions, ClassViewInstanceProvider instanceProvider) {
    myDependencyListener.setInstanceProvider(instanceProvider);
    Ordering ordering = new Ordering(instanceProvider, myDependencyListener, false);
    for (Abstract.Definition definition : definitions) {
      ordering.doOrder(definition);
    }
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
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        statement.accept(this, null);
      }
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, Void params) {
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        statement.accept(this, null);
      }
      for (Abstract.Definition definition : def.getInstanceDefinitions()) {
        orderDefinition(definition);
      }
      return null;
    }

    @Override
    public Void visitDefine(Abstract.DefineStatement stat, Void params) {
      orderDefinition(stat.getDefinition());
      return null;
    }

    public void orderDefinition(Abstract.Definition definition) {
      ordering.doOrder(definition);
      definition.accept(this, null);
    }
  }
}
