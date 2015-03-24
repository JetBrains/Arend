package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class LamExpression extends Expression implements Abstract.LamExpression {
  private final String myVariable;
  private final Expression myBody;

  public LamExpression(String variable, Expression body) {
    myVariable = variable;
    myBody = body;
  }

  @Override
  public String getVariable() {
    return myVariable;
  }

  @Override
  public Expression getBody() {
    return myBody;
  }

  @Override
  public String toString() {
    return "\\" + myVariable + " -> " + myBody;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitLam(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }
}
