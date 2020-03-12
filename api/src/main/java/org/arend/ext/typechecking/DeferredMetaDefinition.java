package org.arend.ext.typechecking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeferredMetaDefinition extends BaseMetaDefinition {
  private final ExpressionTypechecker.Stage stage;
  private final MetaDefinition deferredMeta;

  public DeferredMetaDefinition(ExpressionTypechecker.Stage stage, MetaDefinition deferredMeta) {
    this.stage = stage;
    this.deferredMeta = deferredMeta;
  }

  @Override
  protected boolean requireExpectedType() {
    return true;
  }

  @Nullable
  @Override
  public CheckedExpression invoke(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    if (deferredMeta instanceof BaseMetaDefinition && !((BaseMetaDefinition) deferredMeta).checkContextData(contextData, typechecker.getErrorReporter()) || !checkContextData(contextData, typechecker.getErrorReporter())) {
      return null;
    }
    return typechecker.defer(deferredMeta, contextData, contextData.getExpectedType(), stage);
  }
}
