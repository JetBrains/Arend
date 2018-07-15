package com.jetbrains.jetpad.vclang.core.expr;

import java.math.BigInteger;

public class SmallIntegerExpression extends IntegerExpression {
  private final int myInteger;

  public SmallIntegerExpression(int integer) {
    myInteger = integer;
  }

  public int getInteger() {
    return myInteger;
  }

  @Override
  public BigInteger getBigInteger() {
    return BigInteger.valueOf(myInteger);
  }

  @Override
  public IntegerExpression suc() {
    if (myInteger < 0) {
      return new SmallIntegerExpression(myInteger + 1);
    }
    int newInt = myInteger + 1;
    return newInt < 0 ? new BigIntegerExpression(BigInteger.valueOf(myInteger).add(BigInteger.ONE)) : new SmallIntegerExpression(newInt);
  }

  @Override
  public IntegerExpression pred() {
    if (myInteger > 0) {
      return new SmallIntegerExpression(myInteger - 1);
    }
    int newInt = myInteger - 1;
    return newInt > 0 ? new BigIntegerExpression(BigInteger.valueOf(myInteger).subtract(BigInteger.ONE)) : new SmallIntegerExpression(newInt);
  }

  @Override
  public boolean isZero() {
    return myInteger == 0;
  }

  @Override
  public boolean isEqual(IntegerExpression expr) {
    return expr instanceof SmallIntegerExpression ? myInteger == ((SmallIntegerExpression) expr).getInteger() : expr.getBigInteger().equals(BigInteger.valueOf(myInteger));
  }
}
