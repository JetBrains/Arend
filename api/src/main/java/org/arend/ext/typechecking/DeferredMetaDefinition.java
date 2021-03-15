package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A deferred meta definition is invoked after inference variables are solved.
 */
public class DeferredMetaDefinition extends BaseMetaDefinition {
  private final boolean allowNotDeferred;
  private final boolean afterLevels;
  public final MetaDefinition deferredMeta;

  public DeferredMetaDefinition(MetaDefinition deferredMeta, boolean allowNotDeferred, boolean afterLevels) {
    this.deferredMeta = deferredMeta;
    this.allowNotDeferred = allowNotDeferred;
    this.afterLevels = afterLevels;
  }

  public DeferredMetaDefinition(MetaDefinition deferredMeta, boolean allowNotDeferred) {
    this(deferredMeta, allowNotDeferred, false);
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
  public int @Nullable [] desugarArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    return deferredMeta.desugarArguments(arguments);
  }

  @Override
  public @Nullable TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    CoreExpression expectedType = contextData.getExpectedType();
    return expectedType == null ? deferredMeta.checkAndInvokeMeta(typechecker, contextData) : typechecker.defer(deferredMeta, contextData, expectedType, afterLevels);
  }
}
