package org.arend.ext.concrete.expr;

import javax.annotation.Nonnull;
import java.util.List;

public interface ConcreteTupleExpression extends ConcreteExpression {
  @Nonnull List<? extends ConcreteExpression> getFields();
}
