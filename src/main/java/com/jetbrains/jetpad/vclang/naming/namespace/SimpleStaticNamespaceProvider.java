package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod.STATIC;

public class SimpleStaticNamespaceProvider implements StaticNamespaceProvider {
  public static final StaticNamespaceProvider INSTANCE = new SimpleStaticNamespaceProvider();

  private final Map<Abstract.Definition, Namespace> cache = new HashMap<>();

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
      if (STATIC.equals(defst.getStaticMod())) {
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
    Namespace ns = cache.get(definition);
    if (ns != null) return ns;

    ns = definition.accept(DefinitionGetNamespaceVisitor.INSTANCE, null);
    cache.put(definition, ns);
    return ns;
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

    @Override
    public Namespace visitImplement(Abstract.ImplementDefinition def, Void params) {
      return new EmptyNamespace();
    }
  }
}
