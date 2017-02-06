package com.jetbrains.jetpad.vclang.typechecking.typeclass.scope;

import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InstanceScopeProvider {
  private final Map<Abstract.Definition, Scope> cache = new HashMap<>();

  private static void forStatements(Collection<? extends Abstract.Statement> statements, SimpleInstanceScope ns) {
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (defst.getDefinition() instanceof Abstract.ClassViewInstance) {
        ns.addInstance((Abstract.ClassViewInstance) defst.getDefinition());
      }
    }
  }

  public Scope forDefinition(Abstract.Definition definition) {
    if (!(definition instanceof Abstract.StatementCollection)) {
      return new EmptyScope();
    }

    Scope ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleInstanceScope sns = new SimpleInstanceScope();
    forStatements(((Abstract.StatementCollection) definition).getGlobalStatements(), sns);
    cache.put(definition, sns);
    return sns;
  }
}
