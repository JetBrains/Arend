package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface CorePiExpression extends CoreExpression {
  @NotNull @Contract(pure = true) CoreParameter getParameters();
  @NotNull @Contract(pure = true) CoreExpression getCodomain();
}
