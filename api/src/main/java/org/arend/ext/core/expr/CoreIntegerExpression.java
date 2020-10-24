package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public interface CoreIntegerExpression extends CoreExpression {
  @NotNull BigInteger getBigInteger();
}
