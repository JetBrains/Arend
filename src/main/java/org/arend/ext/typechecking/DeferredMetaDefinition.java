package org.arend.ext.typechecking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DeferredMetaDefinition extends BaseMetaDefinition {
  private final ExpressionTypechecker.Stage stage;
  private final MetaDefinition deferredMeta;

  public DeferredMetaDefinition(ExpressionTypechecker.Stage stage, MetaDefinition deferredMeta) {
    this.stage = stage;
    this.deferredMeta = deferredMeta;
  }

  @Override
  protected boolean withoutLevels() {
    return false;
  }

  @Override
  protected boolean requiredExpectedType() {
    return true;
  }

  @Nullable
  @Override
  public CheckedExpression invoke(@Nonnull ExpressionTypechecker typechecker, @Nonnull ContextData contextData) {
    if (deferredMeta instanceof BaseMetaDefinition && !((BaseMetaDefinition) deferredMeta).checkContext(contextData) || !checkContext(contextData)) {
      return null;
    }
    return typechecker.defer(deferredMeta, contextData, contextData.getExpectedType(), stage);
  }
}
