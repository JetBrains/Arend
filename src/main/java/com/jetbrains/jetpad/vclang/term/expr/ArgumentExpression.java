package com.jetbrains.jetpad.vclang.term.expr;

public class ArgumentExpression {
  private final Expression myExpression;
  private final boolean myExplicit;
  private final boolean myHidden;

  public ArgumentExpression(Expression expression, boolean explicit, boolean hidden) {
    myExpression = expression;
    myExplicit = explicit;
    myHidden = hidden;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public boolean isExplicit() {
    return myExplicit;
  }

  public boolean isHidden() {
    return myHidden;
  }
}
