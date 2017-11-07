package com.jetbrains.jetpad.vclang.core.expr;

public class TypedLetClause extends LetClause {
  private final Expression myType;

  public TypedLetClause(String name, Expression expression, Expression type) {
    super(name, expression);
    myType = type;
  }

  @Override
  public Expression getTypeExpr() {
    return myType;
  }
}
