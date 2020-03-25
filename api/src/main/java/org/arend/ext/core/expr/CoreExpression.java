package org.arend.ext.core.expr;

import org.arend.ext.core.body.CoreBody;
import org.arend.ext.core.context.CoreInferenceVariable;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.ExpressionMapper;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrintable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreExpression extends CoreBody, PrettyPrintable {
  <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params);

  boolean isError();
  @NotNull CoreExpression getUnderlyingExpression();
  @Nullable CoreInferenceVariable getStuckInferenceVariable();
  @Nullable CoreExpression getType();
  @NotNull CoreExpression normalize(@NotNull NormalizationMode mode);

  @Nullable CoreExpression replaceSubexpressions(@NotNull ExpressionMapper mapper);
  boolean compare(@NotNull CoreExpression expr2, @NotNull CMP cmp);

  @Nullable CoreExpression removeConstLam();
  @Nullable CoreFunCallExpression toEquality();
}
