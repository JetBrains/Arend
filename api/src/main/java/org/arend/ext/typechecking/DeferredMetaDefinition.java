package org.arend.ext.typechecking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A deferred meta definition is invoked after inference variables are solved.
 */
public class DeferredMetaDefinition extends BaseMetaDefinition {
  private final MetaDefinition deferredMeta;

  public DeferredMetaDefinition(MetaDefinition deferredMeta) {
    this.deferredMeta = deferredMeta;
  }

  @Override
  public boolean requireExpectedType() {
    return true;
  }

  @Override
  public @Nullable TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    return typechecker.defer(deferredMeta, contextData, contextData.getExpectedType());
  }
}
