package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.prelude.Prelude;

import java.math.BigInteger;
import java.util.Collections;

public class IntCallExpression extends DataCallExpression {
  private final BigInteger myLowerBound;
  private final BigInteger myUpperBound;

  public IntCallExpression(BigInteger lowerBound, BigInteger upperBound) {
    super(Prelude.INT, Sort.SET0, Collections.emptyList());
    myLowerBound = lowerBound;
    myUpperBound = upperBound;
  }

  public BigInteger getLowerBound() {
    return myLowerBound;
  }

  public BigInteger getUpperBound() {
    return myUpperBound;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitIntCall(this, params);
  }
}
