package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.IgnoredLevelsError;
import org.arend.ext.error.TypecheckingError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Defines a few utility methods that can be used to check the validity of arguments and the expected type.
 */
public abstract class BaseMetaDefinition implements MetaDefinition {
  /**
   * @return  true if this meta definition does not support level annotations
   */
  protected boolean withoutLevels() {
    return false;
  }

  /**
   * @return  an array representing the expected number of arguments and their explicitness; returns {@code null} if the definition can accept any number of arguments without restrictions
   */
  protected @Nullable boolean[] argumentExplicitness() {
    return null;
  }

  /**
   * @return  true if the meta requires the expected type
   */
  protected boolean requireExpectedType() {
    return false;
  }

  /**
   * @return  the number of explicit arguments that can be omitted
   */
  protected int numberOfOptionalExplicitArguments() {
    return 0;
  }

  /**
   * @return  true if meta definitions can accept additional arguments not specified in {@code argumentExplicitness}
   */
  protected boolean allowExcessiveArguments() {
    return !requireExpectedType();
  }

  private boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments, ErrorReporter errorReporter, ConcreteReferenceExpression refExpr) {
    boolean ok = true;
    boolean[] explicitness = argumentExplicitness();
    if (explicitness != null) {
      int i = 0, j = 0;
      while (i < explicitness.length && j < arguments.size()) {
        if (explicitness[i] == arguments.get(j).isExplicit()) {
          i++;
          j++;
        } else if (explicitness[i]) {
          if (errorReporter != null) {
            errorReporter.report(new ArgumentExplicitnessError(true, arguments.get(j).getExpression()));
          }
          j++;
        } else {
          i++;
        }
      }

      if (j < arguments.size() && !allowExcessiveArguments()) {
        if (errorReporter != null) {
          errorReporter.report(new TypecheckingError("Excessive arguments are not allowed for '" + refExpr.getReferent().getRefName() + "'", arguments.get(j).getExpression()));
        }
        ok = false;
      }

      if (i < explicitness.length) {
        int sum = 0;
        for (; i < explicitness.length; i++) {
          if (explicitness[i]) {
            sum++;
          }
        }

        int diff = sum - numberOfOptionalExplicitArguments();
        if (diff > 0) {
          if (errorReporter != null) {
            errorReporter.report(new TypecheckingError("Required " + diff + " more explicit argument" + (diff == 1 ? "" : "s"), refExpr));
          }
          ok = false;
        }
      }
    }
    return ok;
  }

  public boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    return checkArguments(arguments, null, null);
  }

  public boolean checkContextData(@NotNull ContextData contextData, @NotNull ErrorReporter errorReporter) {
    ConcreteReferenceExpression refExpr = contextData.getReferenceExpression();
    if (withoutLevels() && (refExpr.getPLevel() != null || refExpr.getHLevel() != null)) {
      errorReporter.report(new IgnoredLevelsError(refExpr.getPLevel(), refExpr.getHLevel()));
    }

    boolean ok = checkArguments(contextData.getArguments(), errorReporter, refExpr);

    if (contextData.getExpectedType() == null && requireExpectedType()) {
      errorReporter.report(new TypecheckingError("Cannot infer the expected type", refExpr));
      ok = false;
    }

    return ok;
  }

  @Override
  public @Nullable ConcreteExpression checkAndGetConcreteRepresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    return checkArguments(arguments) ? getConcreteRepresentation(arguments) : null;
  }

  @Override
  public @Nullable TypedExpression checkAndInvokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    return checkContextData(contextData, typechecker.getErrorReporter()) ? invokeMeta(typechecker, contextData) : null;
  }
}