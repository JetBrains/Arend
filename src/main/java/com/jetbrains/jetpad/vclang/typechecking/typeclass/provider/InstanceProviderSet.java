package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.naming.scope.DataScope;
import com.jetbrains.jetpad.vclang.naming.scope.FunctionScope;
import com.jetbrains.jetpad.vclang.naming.scope.StaticClassScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceNamespaceProvider;

import java.util.HashMap;
import java.util.Map;

public class InstanceProviderSet {
  private final Map<Abstract.Definition, InstanceProvider> myProviders = new HashMap<>();
  private final InstanceNamespaceProvider myInstanceNamespaceProvider;

  public InstanceProviderSet(InstanceNamespaceProvider instanceNamespaceProvider) {
    myInstanceNamespaceProvider = instanceNamespaceProvider;
  }

  public void setProvider(Abstract.Definition definition, InstanceProvider provider) {
    myProviders.put(definition, provider);
  }

  public InstanceProvider getInstanceProvider(Abstract.Definition definition) {
    InstanceProvider provider = myProviders.get(definition);
    if (provider != null) {
      return provider;
    }

    provider = new SimpleInstanceProvider(getDefinitionScope(definition));
    myProviders.put(definition, provider);
    return provider;
  }

  private Scope getDefinitionScope(Abstract.Definition definition) {
    if (definition == null) {
      return new EmptyScope();
    }

    return definition.accept(new BaseAbstractVisitor<Scope, Scope>() {
      @Override
      public Scope visitFunction(Abstract.FunctionDefinition def, Scope parentScope) {
        return new FunctionScope(parentScope, myInstanceNamespaceProvider.forDefinition(def));
      }

      @Override
      public Scope visitData(Abstract.DataDefinition def, Scope parentScope) {
        return new DataScope(parentScope, myInstanceNamespaceProvider.forDefinition(def));
      }

      @Override
      public Scope visitClass(Abstract.ClassDefinition def, Scope parentScope) {
        return new StaticClassScope(parentScope, myInstanceNamespaceProvider.forDefinition(def));
      }
    }, getDefinitionScope(definition.getParentDefinition()));
  }
}
