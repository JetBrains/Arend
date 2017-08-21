package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.term.Concrete;

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
    forDefinitions(def.getGlobalDefinitions(), ns);
  }

  private static void forData(Abstract.DataDefinition def, SimpleNamespace ns) {
    for (Concrete.ConstructorClause<?> clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        ns.addDefinition(constructor);
      }
    }
  }

  private static void forClass(Abstract.ClassDefinition def, SimpleNamespace ns) {
    forDefinitions(def.getGlobalDefinitions(), ns);
  }

  private static void forClassView(Abstract.ClassView def, SimpleNamespace ns) {
    for (Abstract.ClassViewField field : def.getFields()) {
      ns.addDefinition(field);
    }
  }

  private static void forDefinitions(Collection<? extends Abstract.Definition> definitions, SimpleNamespace ns) {
    for (Abstract.Definition definition : definitions) {
      if (definition instanceof Abstract.ClassView) {
        ns.addDefinition(definition);
        forClassView((Abstract.ClassView) definition, ns);
      } else {
        ns.addDefinition(definition);
        if (definition instanceof Abstract.DataDefinition) {
          forData((Abstract.DataDefinition) definition, ns);  // constructors
        }
      }
    }
  }

  @Override
  public Namespace forReferable(GlobalReferable referable) {
    Abstract.Definition definition = (Abstract.Definition) referable; // TODO[references]

    Namespace ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleNamespace sns = new SimpleNamespace();
    definition.accept(DefinitionGetNamespaceVisitor.INSTANCE, sns);
    cache.put(definition, sns);
    return sns;
  }

  private static class DefinitionGetNamespaceVisitor extends BaseAbstractVisitor<SimpleNamespace, Void> {
    public static final DefinitionGetNamespaceVisitor INSTANCE = new DefinitionGetNamespaceVisitor();

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, SimpleNamespace ns) {
      forFunction(def, ns);
      return null;
    }

    @Override
    public Void visitData(Abstract.DataDefinition def, SimpleNamespace ns) {
      forData(def, ns);
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, SimpleNamespace ns) {
      forClass(def, ns);
      return null;
    }
  }
}
