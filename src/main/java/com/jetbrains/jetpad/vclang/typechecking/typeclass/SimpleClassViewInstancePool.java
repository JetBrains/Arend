package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.HashMap;
import java.util.Map;

public class SimpleClassViewInstancePool implements ClassViewInstancePool {
  private final Map<Expression, Expression> myPool = new HashMap<>();

  @Override
  public Expression getLocalInstance(Expression classifyingExpression) {
    return myPool.get(classifyingExpression.normalize(NormalizeVisitor.Mode.NF));
  }

  public void addLocalInstance(Expression classifyingExpression, Expression instance) {
    myPool.put(classifyingExpression, instance);
  }
}
