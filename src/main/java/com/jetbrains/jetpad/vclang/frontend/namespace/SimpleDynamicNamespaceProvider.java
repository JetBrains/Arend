package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
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

    if (referable instanceof Concrete.ClassDefinition) {
      Concrete.ClassDefinition<?> classDefinition = (Concrete.ClassDefinition) referable;
      ns = forDefinitions(classDefinition.getInstanceDefinitions());
      for (Concrete.ClassField field : classDefinition.getFields()) {
        ns.addDefinition(field);
      }
      classCache.put(referable, ns);

      for (final Concrete.ReferenceExpression superClass : classDefinition.getSuperClasses()) {
        GlobalReferable superDef = Concrete.getUnderlyingClassDef(superClass);
        if (superDef != null) {
          ns.addAll(forReferable(superDef));
        }
      }
    } else if (referable instanceof Concrete.ClassView) {
      ns = new SimpleNamespace();
      Referable classifyingField = ((Concrete.ClassView) referable).getClassifyingField();
      if (classifyingField instanceof GlobalReferable) {
        ns.addDefinition((GlobalReferable) classifyingField);
      }
      for (GlobalReferable viewField : ((Concrete.ClassView<?>) referable).getFields()) {
        ns.addDefinition(viewField);
      }
      classCache.put(referable, ns);
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
