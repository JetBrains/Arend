package org.arend.core.context.binding;

import org.arend.core.expr.Expression;

public interface EvaluatingBinding extends Binding {
  Expression getExpression();
}
