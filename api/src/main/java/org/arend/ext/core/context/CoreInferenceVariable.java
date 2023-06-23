package org.arend.ext.core.context;

import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;

public interface CoreInferenceVariable {
  @NotNull String getName();
  @NotNull TypedExpression computeTyped();
}
