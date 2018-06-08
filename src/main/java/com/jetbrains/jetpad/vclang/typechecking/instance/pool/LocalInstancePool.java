package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements InstancePool {
  static private class Pair {
    final Expression key;
    final Concrete.ClassSynonym classSyn;
    final Expression value;

    public Pair(Expression key, Concrete.ClassSynonym classSyn, Expression value) {
      this.key = key;
      this.classSyn = classSyn;
      this.value = value;
    }
  }

  private final List<Pair> myPool = new ArrayList<>();

  private Expression getInstance(Expression classifyingExpression, Concrete.ClassSynonym classSyn) {
    for (Pair pair : myPool) {
      if (pair.key.equals(classifyingExpression) && pair.classSyn == classSyn) {
        return pair.value;
      }
    }
    return null;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, Concrete.ClassSynonym classSyn, boolean isView) {
    if (isView) {
      return getInstance(classifyingExpression, classSyn);
    } else {
      for (Pair pair : myPool) {
        if (pair.classSyn.getUnderlyingClass().getReferent() == classSyn.getUnderlyingClass().getReferent() && pair.key.equals(classifyingExpression)) {
          return pair.value;
        }
      }
      return null;
    }
  }

  public Expression addInstance(Expression classifyingExpression, Concrete.ClassSynonym classSyn, Expression instance) {
    Expression oldInstance = getInstance(classifyingExpression, classSyn);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new Pair(classifyingExpression, classSyn, instance));
      return null;
    }
  }
}
