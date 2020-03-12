package org.arend.core.expr.visitor;

import org.arend.core.expr.*;

public abstract class BaseExpressionVisitor<P, R> implements ExpressionVisitor<P, R> {
  public abstract R visitDefCall(DefCallExpression expr, P params);

  @Override
  public R visitFunCall(FunCallExpression expr, P params) {
    return visitDefCall(expr, params);
  }

  @Override
  public R visitConCall(ConCallExpression expr, P params) {
    return visitDefCall(expr, params);
  }

  @Override
  public R visitDataCall(DataCallExpression expr, P params) {
    return visitDefCall(expr, params);
  }

  @Override
  public R visitFieldCall(FieldCallExpression expr, P params) {
    return visitDefCall(expr, params);
  }

  @Override
  public R visitClassCall(ClassCallExpression expr, P params) {
    return visitDefCall(expr, params);
  }
}
