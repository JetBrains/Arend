package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class VarExpression extends Expression implements Abstract.VarExpression {
  private final String myName;

  public VarExpression(String name) {
    myName = name;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return (T) this;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public Expression getType(List<Expression> context) {
    return null;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitVar(this, params);
  }
}
