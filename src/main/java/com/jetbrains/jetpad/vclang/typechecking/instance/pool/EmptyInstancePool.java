package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class EmptyInstancePool implements InstancePool {
  public static EmptyInstancePool INSTANCE = new EmptyInstancePool();

  private EmptyInstancePool() {}

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Equations equations, Concrete.SourceNode sourceNode) {
    return null;
  }
}
