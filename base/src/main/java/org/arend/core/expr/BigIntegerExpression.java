package org.arend.core.expr;

import org.arend.prelude.Prelude;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.arend.core.expr.ExpressionFactory.Neg;
import static org.arend.core.expr.ExpressionFactory.Pos;

public class BigIntegerExpression extends IntegerExpression {
  private final BigInteger myInteger;

  public BigIntegerExpression(BigInteger integer) {
    myInteger = integer;
  }

  @NotNull
  @Override
  public BigInteger getBigInteger() {
    return myInteger;
  }

  @Override
  public int getSmallInteger() {
    return myInteger.intValueExact();
  }

  @Override
  public Integer getSmallIntegerOrNull() {
    try {
      return myInteger.intValueExact();
    } catch (ArithmeticException e) {
      return null;
    }
  }

  @Override
  public BigIntegerExpression suc() {
    return new BigIntegerExpression(myInteger.add(BigInteger.ONE));
  }

  @Override
  public IntegerExpression pred() {
    return myInteger.compareTo(BigInteger.ZERO) > 0 ? new BigIntegerExpression(myInteger.subtract(BigInteger.ONE)) : null;
  }

  @Override
  public boolean isZero() {
    return myInteger.equals(BigInteger.ZERO);
  }

  @Override
  public boolean isOne() {
    return myInteger.equals(BigInteger.ONE);
  }

  @Override
  public boolean isEqual(IntegerExpression expr) {
    return expr instanceof SmallIntegerExpression ? myInteger.equals(BigInteger.valueOf(((SmallIntegerExpression) expr).getInteger())) : myInteger.equals(expr.getBigInteger());
  }

  @Override
  public int compare(IntegerExpression expr) {
    return myInteger.compareTo(expr.getBigInteger());
  }

  @Override
  public int compare(int x) {
    return myInteger.compareTo(BigInteger.valueOf(x));
  }

  @Override
  public BigIntegerExpression plus(IntegerExpression expr) {
    return new BigIntegerExpression(myInteger.add(expr.getBigInteger()));
  }

  @Override
  public BigIntegerExpression plus(int num) {
    return new BigIntegerExpression(myInteger.add(BigInteger.valueOf(num)));
  }

  @Override
  public BigIntegerExpression mul(IntegerExpression expr) {
    return new BigIntegerExpression(myInteger.multiply(expr.getBigInteger()));
  }

  @Override
  public ConCallExpression minus(IntegerExpression expr) {
    BigInteger result = myInteger.subtract(expr.getBigInteger());
    return result.signum() >= 0 ? Pos(new BigIntegerExpression(result)) : Neg(new BigIntegerExpression(result.negate()));
  }

  @Override
  public BigIntegerExpression minus(int x) {
    return new BigIntegerExpression(myInteger.subtract(BigInteger.valueOf(x)));
  }

  @Override
  public BigIntegerExpression div(IntegerExpression expr) {
    return expr.isZero() ? this : new BigIntegerExpression(myInteger.divide(expr.getBigInteger()));
  }

  @Override
  public BigIntegerExpression mod(IntegerExpression expr) {
    return expr.isZero() ? this : new BigIntegerExpression(myInteger.remainder(expr.getBigInteger()));
  }

  @Override
  public TupleExpression divMod(IntegerExpression expr) {
    List<Expression> fields = new ArrayList<>(2);
    if (expr.isZero()) {
      fields.add(this);
      fields.add(this);
      return new TupleExpression(fields, Prelude.DIV_MOD_TYPE);
    } else {
      BigInteger[] divMod = myInteger.divideAndRemainder(expr.getBigInteger());
      fields.add(new BigIntegerExpression(divMod[0]));
      fields.add(new BigIntegerExpression(divMod[1]));
      return new TupleExpression(fields, ExpressionFactory.finDivModType(new BigIntegerExpression(divMod[1].add(BigInteger.ONE))));
    }
  }
}
