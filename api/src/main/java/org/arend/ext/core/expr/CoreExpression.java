package org.arend.ext.core.expr;

import org.arend.ext.core.body.CoreBody;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.ExpressionMapper;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface CoreExpression extends CoreBody, UncheckedExpression, PrettyPrintable {
  <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params);

  boolean isError();
  @NotNull CoreExpression getUnderlyingExpression();
  @NotNull CoreExpression computeType();
  @NotNull TypedExpression computeTyped();
  @NotNull CoreExpression normalize(@NotNull NormalizationMode mode);
  @NotNull CoreExpression getPiParameters(@Nullable List<? super CoreParameter> parameters);
  @Nullable UncheckedExpression replaceSubexpressions(@NotNull ExpressionMapper mapper);
  @NotNull UncheckedExpression substitute(@NotNull Map<? extends CoreParameter, ? extends CoreExpression> map);
  boolean compare(@NotNull UncheckedExpression expr2, @NotNull CMP cmp);
  @Nullable CoreExpression removeConstLam();
  @Nullable CoreFunCallExpression toEquality();
}
