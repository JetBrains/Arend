package org.arend.ext.core.definition;

import org.arend.ext.core.body.CoreBody;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreFunctionDefinition extends CoreDefinition {
  enum Kind { FUNC, SFUNC, TYPE, LEMMA, INSTANCE }

  @NotNull Kind getKind();
  CoreExpression getResultType();
  @Nullable CoreExpression getResultTypeLevel();
  @Nullable CoreBody getBody();
  @Nullable CoreBody getActualBody();
}
