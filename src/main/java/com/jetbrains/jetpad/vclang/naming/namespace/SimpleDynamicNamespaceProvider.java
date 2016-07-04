package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod.STATIC;

public class SimpleDynamicNamespaceProvider implements DynamicNamespaceProvider {
  public static final DynamicNamespaceProvider INSTANCE = new SimpleDynamicNamespaceProvider();

  private final Map<Abstract.ClassDefinition, SimpleNamespace> classCache = new HashMap<>();

  @Override
  public SimpleNamespace forClass(final Abstract.ClassDefinition classDefinition) {
    SimpleNamespace ns = classCache.get(classDefinition);
    if (ns != null) return ns;

    ns = new SimpleNamespace();
    for (final Abstract.SuperClass superClass : classDefinition.getSuperClasses()) {
      if (superClass.getReferent() instanceof Abstract.ClassDefinition) {
        SimpleNamespace namespace = forClass((Abstract.ClassDefinition) superClass.getReferent());
        loop:
        for (Map.Entry<String, Referable> entry : namespace.getEntrySet()) {
          String name = entry.getKey();
          Referable referable = entry.getValue();
          if (superClass.getRenamings() != null) {
            for (Abstract.IdPair idPair : superClass.getRenamings()) {
              if (referable.equals(idPair.getFirstReferent())) {
                name = idPair.getSecondName();
                break;
              }
            }
          }
          if (superClass.getHidings() != null) {
            for (Abstract.Identifier identifier : superClass.getHidings()) {
              if (referable.equals(identifier.getReferent())) {
                continue loop;
              }
            }
          }
          ns.addDefinition(name, referable);
        }
      } else {
        throw new Namespace.InvalidNamespaceException() {  // FIXME[error] report proper
          @Override
          public GeneralError toError() {
            return new GeneralError("Superclass " + superClass.getName() + " must be a class definition", classDefinition);
          }
        };
      }
    }

    for (Map.Entry<String, Referable> entry : forStatements(classDefinition.getStatements()).getEntrySet()) {
      ns.addDefinition(entry.getKey(), entry.getValue(), entry.getValue() instanceof Abstract.ImplementDefinition);
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

  private static SimpleNamespace forStatements(Collection<? extends Abstract.Statement> statements) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (!STATIC.equals(defst.getStaticMod())) {
        ns.addDefinition(defst.getDefinition());
        if (defst.getDefinition() instanceof Abstract.DataDefinition) {
          ns.addAll(forData((Abstract.DataDefinition) defst.getDefinition()));  // constructors
        }
      }
    }
    return ns;
  }
}
