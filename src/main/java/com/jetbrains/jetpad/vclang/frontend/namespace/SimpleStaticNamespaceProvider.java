package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleStaticNamespaceProvider implements StaticNamespaceProvider {
  private final Map<Concrete.Definition, Namespace> cache = new HashMap<>();

  public static SimpleNamespace forClass(Concrete.ClassDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    forClass(def, ns);
    return ns;
  }

  private static void forFunction(Concrete.FunctionDefinition<?> def, SimpleNamespace ns) {
    forDefinitions(def.getGlobalDefinitions(), ns);
  }

  private static void forData(Concrete.DataDefinition<?> def, SimpleNamespace ns) {
    for (Concrete.ConstructorClause<?> clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        ns.addDefinition(constructor);
      }
    }
  }

  private static void forClass(Concrete.ClassDefinition<?> def, SimpleNamespace ns) {
    forDefinitions(def.getGlobalDefinitions(), ns);
  }

  private static void forClassView(Concrete.ClassView<?> def, SimpleNamespace ns) {
    for (Concrete.ClassViewField field : def.getFields()) {
      ns.addDefinition(field);
    }
  }

  private static void forDefinitions(Collection<? extends Concrete.Definition<?>> definitions, SimpleNamespace ns) {
    for (Concrete.Definition definition : definitions) {
      if (definition instanceof Concrete.ClassView) {
        ns.addDefinition(definition);
        forClassView((Concrete.ClassView) definition, ns);
      } else {
        ns.addDefinition(definition);
        if (definition instanceof Concrete.DataDefinition) {
          forData((Concrete.DataDefinition) definition, ns);  // constructors
        }
      }
    }
  }

  @Override
  public Namespace forReferable(GlobalReferable referable) {
    Concrete.Definition definition = (Concrete.Definition) referable; // TODO[abstract]

    Namespace ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleNamespace sns = new SimpleNamespace();
    definition.accept(DefinitionGetNamespaceVisitor.INSTANCE, sns);
    cache.put(definition, sns);
    return sns;
  }

  private static class DefinitionGetNamespaceVisitor<T> extends BaseAbstractVisitor<T, SimpleNamespace, Void> {
    public static final DefinitionGetNamespaceVisitor INSTANCE = new DefinitionGetNamespaceVisitor();

    @Override
    public Void visitFunction(Concrete.FunctionDefinition<T> def, SimpleNamespace ns) {
      forFunction(def, ns);
      return null;
    }

    @Override
    public Void visitData(Concrete.DataDefinition<T> def, SimpleNamespace ns) {
      forData(def, ns);
      return null;
    }

    @Override
    public Void visitClass(Concrete.ClassDefinition<T> def, SimpleNamespace ns) {
      forClass(def, ns);
      return null;
    }
  }
}
