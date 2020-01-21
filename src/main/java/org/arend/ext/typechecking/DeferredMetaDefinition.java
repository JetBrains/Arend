package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DeferredMetaDefinition extends BaseMetaDefinition {
  protected ContextData contextData;

  @Override
  protected boolean requiredExpectedType() {
    return super.requiredExpectedType();
  }

  @Nullable
  @Override
  public CheckedExpression invoke(@Nonnull ExpressionTypechecker session, @Nonnull ContextData contextData) {
    if (checkContext(contextData)) {
      this.contextData = contextData;
    }
    return null;
  }

  @Nullable
  @Override
  public CoreExpression getExpectedType() {
    return contextData == null ? null : contextData.getExpectedType();
  }
}
