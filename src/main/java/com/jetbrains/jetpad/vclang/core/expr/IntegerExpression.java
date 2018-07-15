package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.prelude.Prelude;

import java.math.BigInteger;

public class IntegerExpression extends Expression {
  private final BigInteger myInteger;

  public IntegerExpression(BigInteger integer) {
    myInteger = integer;
  }

  public BigInteger getInteger() {
    return myInteger;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInteger(this, params);
  }

  @Override
  public boolean isWHNF() {
    return true;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }

  public boolean match(Constructor constructor) {
    return (constructor == Prelude.ZERO || constructor == Prelude.SUC) && (constructor == Prelude.ZERO) == myInteger.equals(BigInteger.ZERO);
  }
}
