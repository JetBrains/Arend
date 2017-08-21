package com.jetbrains.jetpad.vclang.typechecking.typeclass.scope;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InstanceNamespaceProvider<T> {
  private final Map<Abstract.Definition, Scope> cache = new HashMap<>();
  private final ErrorReporter<T> myErrorReporter;

  public InstanceNamespaceProvider(ErrorReporter<T> errorReporter) {
    myErrorReporter = errorReporter;
  }

  private static <T> void forDefinitions(Collection<? extends Concrete.Definition<T>> definitions, SimpleInstanceNamespace<T> ns) {
    for (Concrete.Definition<T> definition : definitions) {
      if (definition instanceof Concrete.ClassViewInstance) {
        ns.addInstance((Concrete.ClassViewInstance<T>) definition);
      }
    }
  }

  public Scope forDefinition(Concrete.Definition<T> definition) {
    if (!(definition instanceof Concrete.DefinitionCollection)) {
      return new EmptyScope();
    }

    Scope ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleInstanceNamespace<T> sns = new SimpleInstanceNamespace<>(myErrorReporter);
    forDefinitions(((Concrete.DefinitionCollection<T>) definition).getGlobalDefinitions(), sns);
    cache.put(definition, sns);
    return sns;
  }
}
