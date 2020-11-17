package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteLetClause;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ConcreteLetExpression extends ConcreteExpression {
  boolean isHave();
  boolean isStrict();
  @NotNull List<? extends ConcreteLetClause> getClauses();
  @NotNull ConcreteExpression getExpression();
}
