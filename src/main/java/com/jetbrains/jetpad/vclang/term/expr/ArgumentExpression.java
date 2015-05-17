package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class ArgumentExpression implements Abstract.ArgumentExpression {
  private final Expression myExpression;
  private final boolean myExplicit;
  private final boolean myHidden;

  public ArgumentExpression(Expression expression, boolean explicit, boolean hidden) {
    myExpression = expression;
    myExplicit = explicit;
    myHidden = hidden;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public boolean isExplicit() {
    return myExplicit;
  }

  @Override
  public boolean isHidden() {
    return myHidden;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    myExpression.prettyPrint(builder, names, prec);
  }
}
