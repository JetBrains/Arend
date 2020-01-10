package org.arend.ext.core.expr;

import org.arend.ext.core.elimtree.CoreBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CoreExpression extends CoreBody {
  <P, R> R accept(@Nonnull CoreExpressionVisitor<? super P, ? extends R> visitor, P params);

  @Nonnull CoreExpression getUnderlyingExpression();
  @Nullable CoreExpression getType();
}
