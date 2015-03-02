package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class PiExpression extends Expression implements Abstract.PiExpression {
  private final boolean explicit;
  private final String variable;
  private final Expression domain;
  private final Expression codomain;

  public PiExpression(Expression domain, Expression codomain) {
    this(true, null, domain, codomain.liftIndex(0, 1));
  }

  public PiExpression(boolean explicit, String variable, Expression domain, Expression codomain) {
    this.explicit = explicit;
    this.variable = variable;
    this.domain = domain;
    this.codomain = codomain;
  }

  @Override
  public String getVariable() {
    return variable;
  }

  @Override
  public Expression getDomain() {
    return domain;
  }

  @Override
  public Expression getCodomain() {
    return codomain;
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
    return domain.equals(other.domain) && codomain.equals(other.codomain);
  }

  @Override
  public String toString() {
    return "(" + (variable == null ? "" : variable + " : ") + domain.toString() + ") -> " + codomain.toString();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitPi(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }
}
