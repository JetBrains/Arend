package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.ext.core.context.CoreEvaluatingBinding;
import org.jetbrains.annotations.NotNull;

public interface EvaluatingBinding extends Binding, CoreEvaluatingBinding {
  @NotNull @Override Expression getExpression();
}
