package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.naming.scope.DataScope;
import com.jetbrains.jetpad.vclang.naming.scope.FunctionScope;
import com.jetbrains.jetpad.vclang.naming.scope.StaticClassScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceScopeProvider;

import java.util.HashMap;
import java.util.Map;

public class ClassViewInstanceProviderProvider {
  private final Map<Abstract.Definition, ClassViewInstanceProvider> myProviders = new HashMap<>();
  private final InstanceScopeProvider myInstanceScopeProvider;

  public ClassViewInstanceProviderProvider(InstanceScopeProvider instanceScopeProvider) {
    myInstanceScopeProvider = instanceScopeProvider;
  }

  public void addProvider(Abstract.Definition definition, ClassViewInstanceProvider provider) {
    myProviders.put(definition, provider);
  }

  public ClassViewInstanceProvider getInstanceProvider(Abstract.Definition definition) {
    ClassViewInstanceProvider provider = myProviders.get(definition);
    if (provider != null) {
      return provider;
    }

    provider = new SimpleClassViewInstanceProvider(getDefinitionScope(definition));
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
        return new FunctionScope(parentScope, myInstanceScopeProvider.forDefinition(def));
      }

      @Override
      public Scope visitData(Abstract.DataDefinition def, Scope parentScope) {
        return new DataScope(parentScope, myInstanceScopeProvider.forDefinition(def));
      }

      @Override
      public Scope visitClass(Abstract.ClassDefinition def, Scope parentScope) {
        return new StaticClassScope(parentScope, myInstanceScopeProvider.forDefinition(def));
      }
    }, getDefinitionScope(definition.getParentDefinition()));
  }
}
