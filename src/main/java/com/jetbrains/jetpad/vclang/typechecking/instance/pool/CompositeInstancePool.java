package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.Arrays;
import java.util.List;

public class CompositeInstancePool implements InstancePool {
  private final List<InstancePool> myPools;

  public CompositeInstancePool(InstancePool... pools) {
    myPools = Arrays.asList(pools);
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, Concrete.ClassSynonym classSyn, boolean isView) {
    for (InstancePool pool : myPools) {
      Expression expr = pool.getInstance(classifyingExpression, classSyn, isView);
      if (expr != null) {
        return expr;
      }
    }
    return null;
  }
}
