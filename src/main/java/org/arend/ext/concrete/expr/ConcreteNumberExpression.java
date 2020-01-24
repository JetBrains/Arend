package org.arend.ext.concrete.expr;

import javax.annotation.Nonnull;
import java.math.BigInteger;

public interface ConcreteNumberExpression extends ConcreteExpression {
  @Nonnull BigInteger getNumber();
}
