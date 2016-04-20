package com.jetbrains.jetpad.vclang.naming.namespace.provider;

import com.jetbrains.jetpad.vclang.naming.namespace.ClassNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.FunctionNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;

public class StatelessNamespaceProvider implements NamespaceProvider {
  private final static AbstractDefinitionVisitor<Void, Namespace> visitor = new AbstractDefinitionVisitor<Void, Namespace>() {
    @Override
    public Namespace visitFunction(Abstract.FunctionDefinition def, Void params) {
      return new FunctionNamespace(def);
    }

    @Override
    public Namespace visitAbstract(Abstract.AbstractDefinition def, Void params) {
      return new EmptyNamespace();
    }

    @Override
    public Namespace visitData(Abstract.DataDefinition def, Void params) {
      return new EmptyNamespace();
    }

    @Override
    public Namespace visitConstructor(Abstract.Constructor def, Void params) {
      return new EmptyNamespace();
    }

    @Override
    public Namespace visitClass(Abstract.ClassDefinition def, Void params) {
      return new ClassNamespace(def);
    }
  };

  @Override
  public Namespace forDefinition(Abstract.Definition definition) {
    return definition.accept(visitor, null);
  }
}
