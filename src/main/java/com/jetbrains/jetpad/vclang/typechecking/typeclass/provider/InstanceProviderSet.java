package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.naming.scope.DataScope;
import com.jetbrains.jetpad.vclang.naming.scope.FunctionScope;
import com.jetbrains.jetpad.vclang.naming.scope.StaticClassScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceNamespaceProvider;

import java.util.HashMap;
import java.util.Map;

public class InstanceProviderSet<T> {
  private final Map<Abstract.Definition, InstanceProvider> myProviders = new HashMap<>();
  private final InstanceNamespaceProvider<T> myInstanceNamespaceProvider;

  public InstanceProviderSet(InstanceNamespaceProvider<T> instanceNamespaceProvider) {
    myInstanceNamespaceProvider = instanceNamespaceProvider;
  }

  public void setProvider(Abstract.Definition definition, InstanceProvider provider) {
    myProviders.put(definition, provider);
  }

  public InstanceProvider getInstanceProvider(Concrete.Definition<T> definition) {
    InstanceProvider provider = myProviders.get(definition);
    if (provider != null) {
      return provider;
    }

    provider = new SimpleInstanceProvider(getDefinitionScope(definition));
    myProviders.put(definition, provider);
    return provider;
  }

  private Scope getDefinitionScope(Concrete.Definition<T> definition) {
    if (definition == null) {
      return new EmptyScope();
    }

    return definition.accept(new BaseAbstractVisitor<T, Scope, Scope>() {
      @Override
      public Scope visitFunction(Concrete.FunctionDefinition<T> def, Scope parentScope) {
        return new FunctionScope(parentScope, myInstanceNamespaceProvider.forDefinition(def));
      }

      @Override
      public Scope visitData(Concrete.DataDefinition<T> def, Scope parentScope) {
        return new DataScope(parentScope, myInstanceNamespaceProvider.forDefinition(def));
      }

      @Override
      public Scope visitClass(Concrete.ClassDefinition<T> def, Scope parentScope) {
        return new StaticClassScope(parentScope, myInstanceNamespaceProvider.forDefinition(def));
      }
    }, getDefinitionScope(definition.getParentDefinition()));
  }
}
