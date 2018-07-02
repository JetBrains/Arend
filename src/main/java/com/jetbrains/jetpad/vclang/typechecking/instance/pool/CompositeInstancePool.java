package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.Arrays;
import java.util.List;

public class CompositeInstancePool implements InstancePool {
  private final List<InstancePool> myPools;

  public CompositeInstancePool(InstancePool... pools) {
    myPools = Arrays.asList(pools);
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, boolean isField, Equations equations, Concrete.SourceNode sourceNode) {
    for (InstancePool pool : myPools) {
      Expression expr = pool.getInstance(classifyingExpression, classRef, isField, equations, sourceNode);
      if (expr != null) {
        return expr;
      }
    }
    return null;
  }
}
