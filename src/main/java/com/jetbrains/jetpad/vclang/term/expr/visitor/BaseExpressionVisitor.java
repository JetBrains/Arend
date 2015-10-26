package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;

public abstract class BaseExpressionVisitor<T> implements ExpressionVisitor<T> {
  public abstract T visitDefCall(DefCallExpression expr);

  @Override
  public T visitFunCall(FunCallExpression expr) {
    return visitDefCall(expr);
  }

  @Override
  public T visitConCall(ConCallExpression expr) {
    return visitDefCall(expr);
  }

  @Override
  public T visitDataCall(DataCallExpression expr) {
    return visitDefCall(expr);
  }

  @Override
  public T visitFieldCall(FieldCallExpression expr) {
    return visitDefCall(expr);
  }

  @Override
  public T visitClassCall(ClassCallExpression expr) {
    return visitDefCall(expr);
  }
}
