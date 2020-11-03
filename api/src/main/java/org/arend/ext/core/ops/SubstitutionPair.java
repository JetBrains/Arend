package org.arend.ext.core.ops;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.context.CoreBinding;

public class SubstitutionPair {
  public final CoreBinding binding;
  public final ConcreteExpression expression;

  public SubstitutionPair(CoreBinding binding, ConcreteExpression expression) {
    this.binding = binding;
    this.expression = expression;
  }
}
