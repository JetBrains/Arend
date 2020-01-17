package org.arend.ext.core.expr;

import org.arend.ext.core.elimtree.CoreBody;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.ExpressionMapper;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrintable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CoreExpression extends CoreBody, PrettyPrintable {
  <P, R> R accept(@Nonnull CoreExpressionVisitor<? super P, ? extends R> visitor, P params);

  @Nonnull CoreExpression getUnderlyingExpression();
  @Nullable CoreExpression getType();
  @Nonnull CoreExpression normalize(@Nonnull NormalizationMode mode);

  @Nullable CoreExpression recreate(@Nonnull ExpressionMapper mapper);
  boolean compare(@Nonnull CoreExpression expr2, @Nonnull CMP cmp);
}
