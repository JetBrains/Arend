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
  private final DefermentChecker checker;
  public final MetaDefinition deferredMeta;

  public DeferredMetaDefinition(MetaDefinition deferredMeta, boolean allowNotDeferred, DefermentChecker checker) {
    this.deferredMeta = deferredMeta;
    this.allowNotDeferred = allowNotDeferred;
    this.afterLevels = false;
    this.checker = checker;
  }

  public DeferredMetaDefinition(MetaDefinition deferredMeta, boolean allowNotDeferred, boolean afterLevels) {
    this.deferredMeta = deferredMeta;
    this.allowNotDeferred = allowNotDeferred;
    this.afterLevels = afterLevels;
    this.checker = null;
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
    if (expectedType == null) {
      return deferredMeta.checkAndInvokeMeta(typechecker, contextData);
    }
    if (checker != null) {
      DefermentChecker.Result result = checker.check(typechecker, contextData);
      if (result == null) {
        return null;
      } else if (result == DefermentChecker.Result.DO_NOT_DEFER) {
        return deferredMeta.checkAndInvokeMeta(typechecker, contextData);
      } else {
        boolean afterLevels = result == DefermentChecker.Result.AFTER_LEVELS;
        return typechecker.defer(afterLevels ? deferredMeta : this, contextData, expectedType, afterLevels);
      }
    } else {
      return typechecker.defer(deferredMeta, contextData, expectedType, afterLevels);
    }
  }
}
