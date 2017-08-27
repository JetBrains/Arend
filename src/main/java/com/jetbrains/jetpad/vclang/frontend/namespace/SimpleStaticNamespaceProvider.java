package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
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

  @Nonnull
  @Override
  public Namespace forReferable(GlobalReferable referable) {
    if (referable instanceof GlobalReference) {
      referable = ((GlobalReference) referable).getDefinition();
    }
    if (!(referable instanceof Concrete.Definition)) {
      return new EmptyNamespace();
    }

    Concrete.Definition definition = (Concrete.Definition) referable; // TODO[abstract]
    Namespace ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleNamespace sns = new SimpleNamespace();
    definition.accept(DefinitionGetNamespaceVisitor.INSTANCE, sns);
    cache.put(definition, sns);
    return sns;
  }

  public void collect(Group group) {

  }
}
