package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;

import static com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod.DYNAMIC;

public class SimpleDynamicNamespaceProvider implements DynamicNamespaceProvider {
  @Override
  public Namespace forClass(Abstract.ClassDefinition classDefinition) {
    return forStatements(classDefinition.getStatements());
  }

  private static SimpleNamespace forStatements(Collection<? extends Abstract.Statement> statements) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (DYNAMIC.equals(defst.getStaticMod())) {
        ns.addDefinition(defst.getDefinition());
      }
    }
    return ns;
  }
}
