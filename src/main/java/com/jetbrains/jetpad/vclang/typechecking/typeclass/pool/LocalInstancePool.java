package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements ClassViewInstancePool {
  static private class Pair {
    final Expression key;
    final Abstract.ClassView classView;
    final Expression value;

    public Pair(Expression key, Abstract.ClassView classView, Expression value) {
      this.key = key;
      this.classView = classView;
      this.value = value;
    }
  }

  private final List<Pair> myPool = new ArrayList<>();

  private Expression getInstance(Expression classifyingExpression, Abstract.ClassView classView) {
    Expression expr = classifyingExpression.normalize(NormalizeVisitor.Mode.NF);
    for (Pair pair : myPool) {
      if (pair.key.equals(expr) && pair.classView == classView) {
        return pair.value;
      }
    }
    return null;
  }

  @Override
  public Expression getInstance(Abstract.ReferenceExpression defCall, Expression classifyingExpression, Abstract.ClassView classView) {
    return getInstance(classifyingExpression, classView);
  }

  @Override
  public Expression getInstance(Abstract.ReferenceExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.ClassDefinition classDefinition) {
    Expression expr = classifyingExpression.normalize(NormalizeVisitor.Mode.NF);
    for (Pair pair : myPool) {
      if (pair.key.equals(expr) && pair.classView.getUnderlyingClassReference().getReferent() == classDefinition) {
        return pair.value;
      }
    }
    return null;
  }

  public boolean addInstance(Expression classifyingExpression, Abstract.ClassView classView, Expression instance) {
    if (getInstance(classifyingExpression, classView) != null) {
      return false;
    } else {
      myPool.add(new Pair(classifyingExpression, classView, instance));
      return true;
    }
  }
}
