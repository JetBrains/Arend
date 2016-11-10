package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleDynamicNamespaceProvider implements DynamicNamespaceProvider {
  private final Map<Abstract.ClassDefinition, SimpleNamespace> classCache = new HashMap<>();

  @Override
  public SimpleNamespace forClass(final Abstract.ClassDefinition classDefinition) {
    SimpleNamespace ns = classCache.get(classDefinition);
    if (ns != null) return ns;

    ns = forDefinitions(classDefinition.getInstanceDefinitions());

    for (Abstract.ClassField field : classDefinition.getFields()) {
      ns.addDefinition(field);
    }

    for (final Abstract.SuperClass superClass : classDefinition.getSuperClasses()) {
      Abstract.ClassDefinition superDef = Abstract.getUnderlyingClassDef(superClass.getSuperClass());
      if (superDef != null) {
        ns.addAll(forClass(superDef));
      }
    }

    classCache.put(classDefinition, ns);
    return ns;
  }

  private static SimpleNamespace forData(Abstract.DataDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Constructor constructor : def.getConstructors()) {
      ns.addDefinition(constructor);
    }
    return ns;
  }

  private static SimpleNamespace forDefinitions(Collection<? extends Abstract.Definition> definitions) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Definition definition : definitions) {
      ns.addDefinition(definition);
      if (definition instanceof Abstract.DataDefinition) {
        ns.addAll(forData((Abstract.DataDefinition) definition));  // constructors
      }
    }
    return ns;
  }
}
