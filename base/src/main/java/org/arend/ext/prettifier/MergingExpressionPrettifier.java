package org.arend.ext.prettifier;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MergingExpressionPrettifier implements ExpressionPrettifier {
  private final List<ExpressionPrettifier> myPrettifiers;

  public MergingExpressionPrettifier(List<ExpressionPrettifier> prettifiers) {
    myPrettifiers = prettifiers;
  }

  @Override
  public @Nullable ConcreteExpression prettify(@NotNull CoreExpression expression, @NotNull ExpressionPrettifier defaultPrettifier) {
    for (ExpressionPrettifier prettifier : myPrettifiers) {
      ConcreteExpression result = prettifier.prettify(expression, defaultPrettifier);
      if (result != null) return result;
    }
    return null;
  }
}
