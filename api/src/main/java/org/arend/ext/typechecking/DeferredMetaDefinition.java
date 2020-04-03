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

  @Override
  public @Nullable TypedExpression invoke(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    return typechecker.defer(deferredMeta, contextData, contextData.getExpectedType(), stage);
  }
}
