package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;

import java.util.Collection;

public class SimpleStaticNamespaceProvider implements StaticNamespaceProvider {
  public static SimpleNamespace forFunction(Abstract.FunctionDefinition def) {
    return forStatements(def.getStatements());
  }

  public static SimpleNamespace forData(Abstract.DataDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Constructor constructor : def.getConstructors()) {
      ns.addDefinition(constructor);
    }
    return ns;
  }

  public static SimpleNamespace forClass(Abstract.ClassDefinition def) {
    return forStatements(def.getStatements());
  }

  private static SimpleNamespace forStatements(Collection<? extends Abstract.Statement> statements) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (!Abstract.DefineStatement.StaticMod.DYNAMIC.equals(defst.getStaticMod())) {
        ns.addDefinition(defst.getDefinition());
        if (defst.getDefinition() instanceof Abstract.DataDefinition) {
          ns.addAll(forData((Abstract.DataDefinition) defst.getDefinition()));  // constructors
        }
      }
    }
    return ns;
  }

  @Override
  public Namespace forDefinition(Abstract.Definition definition) {
    return definition.accept(DefinitionGetNamespaceVisitor.INSTANCE, null);
  }

  private static class DefinitionGetNamespaceVisitor implements AbstractDefinitionVisitor<Void, Namespace> {
    public static final DefinitionGetNamespaceVisitor INSTANCE = new DefinitionGetNamespaceVisitor();

    @Override
    public Namespace visitFunction(Abstract.FunctionDefinition def, Void params) {
      return forFunction(def);
    }

    @Override
    public Namespace visitAbstract(Abstract.AbstractDefinition def, Void params) {
      return new EmptyNamespace();
    }

    @Override
    public Namespace visitData(Abstract.DataDefinition def, Void params) {
      return forData(def);
    }

    @Override
    public Namespace visitConstructor(Abstract.Constructor def, Void params) {
      return new EmptyNamespace();
    }

    @Override
    public Namespace visitClass(Abstract.ClassDefinition def, Void params) {
      return forClass(def);
    }
  }
}
