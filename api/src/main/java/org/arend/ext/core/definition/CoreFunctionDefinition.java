package org.arend.ext.core.definition;

import org.arend.ext.core.body.CoreBody;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreFunctionDefinition extends CoreDefinition {
  enum Kind { FUNC, SFUNC, LEMMA }

  @NotNull Kind getKind();
  @NotNull CoreParameter getParameters();
  CoreExpression getResultType();
  @Nullable CoreExpression getResultTypeLevel();
  @Nullable CoreBody getActualBody();
}
