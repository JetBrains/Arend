package org.arend.ext.core.expr;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

public interface CoreQNameExpression extends CoreExpression {
  @NotNull ArendRef getRef();
}
