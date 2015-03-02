package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class LamExpression extends Expression implements Abstract.LamExpression {
  private final String variable;
  private final Expression body;

  public LamExpression(String variable, Expression expression) {
    this.variable = variable;
    this.body = expression;
  }

  @Override
  public String getVariable() {
    return variable;
  }

  @Override
  public Expression getBody() {
    return body;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof LamExpression)) return false;
    LamExpression other = (LamExpression)o;
    return body.equals(other.body);
  }

  @Override
  public String toString() {
    return "\\" + variable + " -> " + body;
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
