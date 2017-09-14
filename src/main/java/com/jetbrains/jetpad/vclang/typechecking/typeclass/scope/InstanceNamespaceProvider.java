package com.jetbrains.jetpad.vclang.typechecking.typeclass.scope;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InstanceNamespaceProvider {
  private final Map<Concrete.Definition, Scope> cache = new HashMap<>();
  private final ErrorReporter myErrorReporter;

  public InstanceNamespaceProvider(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  private static  void forDefinitions(Collection<? extends Concrete.Definition> definitions, SimpleInstanceNamespace ns) {
    for (Concrete.Definition definition : definitions) {
      if (definition instanceof Concrete.Instance) {
        ns.addInstance((Concrete.Instance) definition);
      }
    }
  }

  public Scope forDefinition(Concrete.Definition definition) {
    if (!(definition instanceof Concrete.DefinitionCollection)) {
      return EmptyScope.INSTANCE;
    }

    Scope ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleInstanceNamespace sns = new SimpleInstanceNamespace(myErrorReporter);
    forDefinitions(((Concrete.DefinitionCollection) definition).getGlobalDefinitions(), sns);
    cache.put(definition, sns);
    return sns;
  }
}
