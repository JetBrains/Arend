package com.jetbrains.jetpad.vclang.typechecking.typeclass.scope;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InstanceNamespaceProvider {
  private final Map<Abstract.Definition, Scope> cache = new HashMap<>();
  private final ErrorReporter myErrorReporter;

  public InstanceNamespaceProvider(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  private static void forDefinitions(Collection<? extends Abstract.Definition> definitions, SimpleInstanceNamespace ns) {
    for (Abstract.Definition definition : definitions) {
      if (definition instanceof Abstract.ClassViewInstance) {
        ns.addInstance((Abstract.ClassViewInstance) definition);
      }
    }
  }

  public Scope forDefinition(Abstract.Definition definition) {
    if (!(definition instanceof Abstract.DefinitionCollection)) {
      return new EmptyScope();
    }

    Scope ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleInstanceNamespace sns = new SimpleInstanceNamespace(myErrorReporter);
    forDefinitions(((Abstract.DefinitionCollection) definition).getGlobalDefinitions(), sns);
    cache.put(definition, sns);
    return sns;
  }
}
