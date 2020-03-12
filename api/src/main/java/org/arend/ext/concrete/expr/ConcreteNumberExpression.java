package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public interface ConcreteNumberExpression extends ConcreteExpression {
  @NotNull BigInteger getNumber();
}
