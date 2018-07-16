package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.prelude.Prelude;

import java.math.BigInteger;

public abstract class IntegerExpression extends Expression {
  public abstract BigInteger getBigInteger();

  public abstract IntegerExpression suc();

  public abstract IntegerExpression pred();

  public abstract boolean isZero();

  public abstract boolean isEqual(IntegerExpression expr);

  public abstract IntegerExpression plus(IntegerExpression expr);

  public abstract IntegerExpression mul(IntegerExpression expr);

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
    return (constructor == Prelude.ZERO || constructor == Prelude.SUC) && (constructor == Prelude.ZERO) == isZero();
  }
}
