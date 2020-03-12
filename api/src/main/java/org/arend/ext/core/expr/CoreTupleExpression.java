package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreTupleExpression extends CoreExpression {
  @NotNull List<? extends CoreExpression> getFields();
  @NotNull CoreSigmaExpression getSigmaType();
}
