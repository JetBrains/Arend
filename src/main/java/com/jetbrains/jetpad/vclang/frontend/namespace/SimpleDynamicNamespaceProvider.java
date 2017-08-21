package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleDynamicNamespaceProvider implements DynamicNamespaceProvider {
  private final Map<GlobalReferable, SimpleNamespace> classCache = new HashMap<>();

  @Override
  public SimpleNamespace forReferable(final GlobalReferable referable) {
    SimpleNamespace ns = classCache.get(referable);
    if (ns != null) return ns;

    Concrete.ClassDefinition<?> classDefinition = (Concrete.ClassDefinition) referable;
    ns = forDefinitions(classDefinition.getInstanceDefinitions());
    for (Concrete.ClassField field : classDefinition.getFields()) {
      ns.addDefinition(field);
    }
    classCache.put(classDefinition, ns);

    for (final Concrete.SuperClass superClass : classDefinition.getSuperClasses()) {
      Concrete.ClassDefinition superDef = Concrete.getUnderlyingClassDef(superClass.getSuperClass());
      if (superDef != null) {
        ns.addAll(forReferable(superDef));
      }
    }

    return ns;
  }

  private static SimpleNamespace forData(Concrete.DataDefinition<?> def) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Concrete.ConstructorClause<?> clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        ns.addDefinition(constructor);
      }
    }
    return ns;
  }

  private static SimpleNamespace forDefinitions(Collection<? extends Concrete.Definition> definitions) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Concrete.Definition definition : definitions) {
      ns.addDefinition(definition);
      if (definition instanceof Concrete.DataDefinition) {
        ns.addAll(forData((Concrete.DataDefinition) definition));  // constructors
      }
    }
    return ns;
  }
}
