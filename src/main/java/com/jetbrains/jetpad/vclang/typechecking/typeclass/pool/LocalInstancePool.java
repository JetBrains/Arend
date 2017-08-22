package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements ClassViewInstancePool {
  static private class Pair {
    final Expression key;
    final Concrete.ClassView classView;
    final Expression value;

    public Pair(Expression key, Concrete.ClassView classView, Expression value) {
      this.key = key;
      this.classView = classView;
      this.value = value;
    }
  }

  private final List<Pair> myPool = new ArrayList<>();

  private Expression getInstance(Expression classifyingExpression, Concrete.ClassView classView) {
    for (Pair pair : myPool) {
      if (pair.key.equals(classifyingExpression) && pair.classView == classView) {
        return pair.value;
      }
    }
    return null;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, Concrete.ClassView classView, boolean isView) {
    if (isView) {
      return getInstance(classifyingExpression, classView);
    } else {
      for (Pair pair : myPool) {
        if (pair.key.equals(classifyingExpression) && pair.classView.getUnderlyingClass().getReferent() == classView.getUnderlyingClass().getReferent()) {
          return pair.value;
        }
      }
      return null;
    }
  }

  public Expression addInstance(Expression classifyingExpression, Concrete.ClassView classView, Expression instance) {
    Expression oldInstance = getInstance(classifyingExpression, classView);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new Pair(classifyingExpression, classView, instance));
      return null;
    }
  }
}
