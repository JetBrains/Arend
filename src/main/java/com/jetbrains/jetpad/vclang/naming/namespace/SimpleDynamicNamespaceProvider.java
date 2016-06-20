package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.Collection;

import static com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod.DYNAMIC;
import static com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod.STATIC;

public class SimpleDynamicNamespaceProvider implements DynamicNamespaceProvider {
  public static final DynamicNamespaceProvider INSTANCE = new SimpleDynamicNamespaceProvider();

  @Override
  public SimpleNamespace forClass(final Abstract.ClassDefinition classDefinition) {
    SimpleNamespace myNamespace = forStatements(classDefinition.getStatements());
    for (final Referable sup : classDefinition.getSuperClasses()) {
      if (sup instanceof Abstract.ClassDefinition) {
        myNamespace.addAll(forClass((Abstract.ClassDefinition) sup));
      } else {
        throw new Namespace.InvalidNamespaceException() {  // FIXME[error] report proper
          @Override
          public GeneralError toError() {
            return new GeneralError("Superclass " + sup.getName() + " must be a class definition", classDefinition);
          }
        };
      }
    }
    return myNamespace;
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
