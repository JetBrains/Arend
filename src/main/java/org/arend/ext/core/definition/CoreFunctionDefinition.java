package org.arend.ext.core.definition;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.elimtree.CoreBody;
import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CoreFunctionDefinition extends CoreDefinition {
  enum Kind { FUNC, SFUNC, LEMMA }

  @Nonnull Kind getKind();
  @Nonnull CoreParameter getParameters();
  CoreExpression getResultType();
  @Nullable CoreExpression getResultTypeLevel();
  @Nullable CoreBody getActualBody();
}
