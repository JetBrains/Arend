package com.jetbrains.jetpad.vclang.core.expr;

import java.math.BigInteger;

public class BigIntegerExpression extends IntegerExpression {
  private final BigInteger myInteger;

  public BigIntegerExpression(BigInteger integer) {
    myInteger = integer;
  }

  @Override
  public BigInteger getBigInteger() {
    return myInteger;
  }

  @Override
  public BigIntegerExpression suc() {
    return new BigIntegerExpression(myInteger.add(BigInteger.ONE));
  }

  @Override
  public IntegerExpression pred() {
    return new BigIntegerExpression(myInteger.subtract(BigInteger.ONE));
  }

  @Override
  public boolean isZero() {
    return myInteger.equals(BigInteger.ZERO);
  }

  @Override
  public boolean isEqual(IntegerExpression expr) {
    return expr instanceof SmallIntegerExpression ? myInteger.equals(BigInteger.valueOf(((SmallIntegerExpression) expr).getInteger())) : myInteger.equals(expr.getBigInteger());
  }
}
