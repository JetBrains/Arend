package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleStaticNamespaceProvider implements StaticNamespaceProvider {
  private final Map<Abstract.Definition, Namespace> cache = new HashMap<>();

  public static SimpleNamespace forClass(Abstract.ClassDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    forClass(def, ns);
    return ns;
  }

  private static void forFunction(Abstract.FunctionDefinition def, SimpleNamespace ns) {
    forStatements(def.getStatements(), ns);
  }

  private static void forData(Abstract.DataDefinition def, SimpleNamespace ns) {
    for (Abstract.Constructor constructor : def.getConstructors()) {
      ns.addDefinition(constructor);
    }
  }

  private static void forClass(Abstract.ClassDefinition def, SimpleNamespace ns) {
    forStatements(def.getGlobalStatements(), ns);
  }

  private static void forClassView(Abstract.ClassView def, SimpleNamespace ns) {
    for (Abstract.ClassViewField field : def.getFields()) {
      ns.addDefinition(field);
    }
  }

  private static void forStatements(Collection<? extends Abstract.Statement> statements, SimpleNamespace ns) {
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (defst.getDefinition() instanceof Abstract.ClassView) {
        ns.addDefinition(defst.getDefinition());
        forClassView((Abstract.ClassView) defst.getDefinition(), ns);
      } else {
        ns.addDefinition(defst.getDefinition());
        if (defst.getDefinition() instanceof Abstract.DataDefinition) {
          forData((Abstract.DataDefinition) defst.getDefinition(), ns);  // constructors
        }
      }
    }
  }

  @Override
  public Namespace forDefinition(Abstract.Definition definition) {
    Namespace ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleNamespace sns = new SimpleNamespace();
    definition.accept(DefinitionGetNamespaceVisitor.INSTANCE, sns);
    cache.put(definition, sns);
    return sns;
  }

  private static class DefinitionGetNamespaceVisitor implements AbstractDefinitionVisitor<SimpleNamespace, Void> {
    public static final DefinitionGetNamespaceVisitor INSTANCE = new DefinitionGetNamespaceVisitor();

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, SimpleNamespace ns) {
      forFunction(def, ns);
      return null;
    }

    @Override
    public Void visitClassField(Abstract.ClassField def, SimpleNamespace ns) {
      return null;
    }

    @Override
    public Void visitData(Abstract.DataDefinition def, SimpleNamespace ns) {
      forData(def, ns);
      return null;
    }

    @Override
    public Void visitConstructor(Abstract.Constructor def, SimpleNamespace ns) {
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, SimpleNamespace ns) {
      forClass(def, ns);
      return null;
    }

    @Override
    public Void visitImplement(Abstract.Implementation def, SimpleNamespace ns) {
      return null;
    }

    @Override
    public Void visitClassView(Abstract.ClassView def, SimpleNamespace ns) {
      return null;
    }

    @Override
    public Void visitClassViewField(Abstract.ClassViewField def, SimpleNamespace params) {
      return null;
    }

    @Override
    public Void visitClassViewInstance(Abstract.ClassViewInstance def, SimpleNamespace params) {
      return null;
    }
  }
}
