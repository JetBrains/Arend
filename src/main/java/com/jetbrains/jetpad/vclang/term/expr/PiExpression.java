package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class PiExpression extends Expression implements Abstract.PiExpression {
  private final boolean explicit;
  private final String variable;
  private final Expression left;
  private final Expression right;

  public PiExpression(Expression left, Expression right) {
    this(true, null, left, right.liftIndex(0, 1));
  }

  public PiExpression(boolean explicit, String variable, Expression left, Expression right) {
    this.explicit = explicit;
    this.variable = variable;
    this.left = left;
    this.right = right;
  }

  @Override
  public String getVariable() {
    return variable;
  }

  @Override
  public Expression getLeft() {
    return left;
  }

  @Override
  public Expression getRight() {
    return right;
  }

  @Override
  public boolean isExplicit() {
    return explicit;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof PiExpression)) return false;
    PiExpression other = (PiExpression)o;
    return left.equals(other.left) && right.equals(other.right);
  }

  @Override
  public String toString() {
    return "(" + (variable == null ? "" : variable + " : ") + left.toString() + ") -> " + right.toString();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitPi(this);
  }

  @Override
  public <T> T accept(AbstractExpressionVisitor<? extends T> visitor) {
    return visitor.visitPi(this);
  }
}
