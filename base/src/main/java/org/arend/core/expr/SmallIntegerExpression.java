package org.arend.core.expr;

import org.arend.prelude.Prelude;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.arend.core.expr.ExpressionFactory.Neg;
import static org.arend.core.expr.ExpressionFactory.Pos;

public class SmallIntegerExpression extends IntegerExpression {
  private final static int MAX_VALUE_TO_MULTIPLY = 45000;

  private final int myInteger;

  public SmallIntegerExpression(int integer) {
    myInteger = integer;
  }

  public int getInteger() {
    return myInteger;
  }

  @NotNull
  @Override
  public BigInteger getBigInteger() {
    return BigInteger.valueOf(myInteger);
  }

  @Override
  public int getSmallInteger() {
    return myInteger;
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
  public boolean isOne() {
    return myInteger == 1;
  }

  @Override
  public boolean isEqual(IntegerExpression expr) {
    return expr instanceof SmallIntegerExpression ? myInteger == ((SmallIntegerExpression) expr).getInteger() : expr.getBigInteger().equals(BigInteger.valueOf(myInteger));
  }

  @Override
  public int compare(IntegerExpression expr) {
    return expr instanceof SmallIntegerExpression ? Integer.compare(myInteger, ((SmallIntegerExpression) expr).getInteger()) : BigInteger.valueOf(myInteger).compareTo(expr.getBigInteger());
  }

  @Override
  public int compare(int x) {
    return Integer.compare(myInteger, x);
  }

  @Override
  public IntegerExpression plus(IntegerExpression expr) {
    if (expr instanceof SmallIntegerExpression) {
      int sum = myInteger + ((SmallIntegerExpression) expr).myInteger;
      if (sum >= 0) {
        return new SmallIntegerExpression(sum);
      }
    }

    return new BigIntegerExpression(BigInteger.valueOf(myInteger).add(expr.getBigInteger()));
  }

  @Override
  public IntegerExpression plus(int num) {
    int sum = myInteger + num;
    return sum >= 0 ? new SmallIntegerExpression(sum) : new BigIntegerExpression(BigInteger.valueOf(myInteger).add(BigInteger.valueOf(num)));
  }

  @Override
  public IntegerExpression mul(IntegerExpression expr) {
    if (expr instanceof SmallIntegerExpression) {
      int other = ((SmallIntegerExpression) expr).getInteger();
      if (myInteger <= MAX_VALUE_TO_MULTIPLY && other <= MAX_VALUE_TO_MULTIPLY) {
        return new SmallIntegerExpression(myInteger * other);
      }
    }

    return new BigIntegerExpression(BigInteger.valueOf(myInteger).multiply(expr.getBigInteger()));
  }

  @Override
  public ConCallExpression minus(IntegerExpression expr) {
    if (expr instanceof SmallIntegerExpression) {
      int result = myInteger - ((SmallIntegerExpression) expr).myInteger;
      return result >= 0 ? Pos(new SmallIntegerExpression(result)) : Neg(new SmallIntegerExpression(-result));
    } else {
      return new BigIntegerExpression(BigInteger.valueOf(myInteger)).minus(expr);
    }
  }

  @Override
  public IntegerExpression minus(int x) {
    assert x <= myInteger;
    return new SmallIntegerExpression(myInteger - x);
  }

  @Override
  public IntegerExpression div(IntegerExpression expr) {
    if (expr.isZero()) {
      return this;
    }
    if (expr instanceof SmallIntegerExpression) {
      int other = ((SmallIntegerExpression) expr).getInteger();
      return new SmallIntegerExpression(myInteger / other);
    }

    return new BigIntegerExpression(BigInteger.valueOf(myInteger).divide(expr.getBigInteger()));
  }

  @Override
  public IntegerExpression mod(IntegerExpression expr) {
    if (expr.isZero()) {
      return this;
    }
    if (expr instanceof SmallIntegerExpression) {
      int other = ((SmallIntegerExpression) expr).getInteger();
      return new SmallIntegerExpression(myInteger % other);
    }

    return new BigIntegerExpression(BigInteger.valueOf(myInteger).remainder(expr.getBigInteger()));
  }

  @Override
  public TupleExpression divMod(IntegerExpression expr) {
    List<Expression> fields = new ArrayList<>(2);
    if (expr.isZero()) {
      fields.add(this);
      fields.add(this);
      return new TupleExpression(fields, Prelude.DIV_MOD_TYPE);
    } else {
      if (expr instanceof SmallIntegerExpression) {
        int other = ((SmallIntegerExpression) expr).getInteger();
        fields.add(new SmallIntegerExpression(myInteger / other));
        fields.add(new SmallIntegerExpression(myInteger % other));
      } else {
        BigInteger[] divMod = BigInteger.valueOf(myInteger).divideAndRemainder(expr.getBigInteger());
        fields.add(new BigIntegerExpression(divMod[0]));
        fields.add(new BigIntegerExpression(divMod[1]));
      }
      return new TupleExpression(fields, ExpressionFactory.finDivModType(expr));
    }
  }
}
