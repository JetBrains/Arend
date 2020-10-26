package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A deferred meta definition is invoked after inference variables are solved.
 */
public class DeferredMetaDefinition extends BaseMetaDefinition {
  private final boolean allowNotDeferred;
  private final ExpressionTypechecker.Stage stage;
  public final MetaDefinition deferredMeta;

  public DeferredMetaDefinition(MetaDefinition deferredMeta, boolean allowNotDeferred, ExpressionTypechecker.Stage stage) {
    this.deferredMeta = deferredMeta;
    this.allowNotDeferred = allowNotDeferred;
    this.stage = stage;
  }

  public DeferredMetaDefinition(MetaDefinition deferredMeta, boolean allowNotDeferred) {
    this(deferredMeta, allowNotDeferred, ExpressionTypechecker.Stage.BEFORE_SOLVER);
  }

  public DeferredMetaDefinition(MetaDefinition deferredMeta) {
    this(deferredMeta, false);
  }

  @Override
  public boolean withoutLevels() {
    return false;
  }

  @Override
  public boolean requireExpectedType() {
    return !allowNotDeferred;
  }

  @Override
  public @Nullable TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    CoreExpression expectedType = contextData.getExpectedType();
    return expectedType == null ? deferredMeta.checkAndInvokeMeta(typechecker, contextData) : typechecker.defer(deferredMeta, contextData, expectedType, stage);
  }
}
