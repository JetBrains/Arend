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
  public boolean withoutLevels() {
    return true;
  }

  /**
   * @return  an array representing the expected number of arguments and their explicitness; returns {@code null} if the definition can accept any number of arguments without restrictions
   */
  public @Nullable boolean[] argumentExplicitness() {
    return null;
  }

  /**
   * @return  true if the meta requires the expected type
   */
  public boolean requireExpectedType() {
    return false;
  }

  /**
   * @return  the number of explicit arguments that can be omitted
   */
  public int numberOfOptionalExplicitArguments() {
    return 0;
  }

  /**
   * @return  true if meta definitions can accept additional arguments not specified in {@code argumentExplicitness}
   */
  public boolean allowExcessiveArguments() {
    return !requireExpectedType();
  }

  private boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments, ErrorReporter errorReporter, ConcreteExpression marker) {
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
          errorReporter.report(new TypecheckingError("Excessive arguments are not allowed", arguments.get(j).getExpression()));
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
            errorReporter.report(new TypecheckingError("Required " + diff + " more explicit argument" + (diff == 1 ? "" : "s"), marker));
          }
          ok = false;
        }
      }
    }
    return ok;
  }

  @Override
  public boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    return checkArguments(arguments, null, null);
  }

  @Override
  public boolean checkContextData(@NotNull ContextData contextData, @NotNull ErrorReporter errorReporter) {
    ConcreteReferenceExpression refExpr = contextData.getReferenceExpression();
    if (withoutLevels() && (refExpr.getPLevel() != null || refExpr.getHLevel() != null)) {
      errorReporter.report(new IgnoredLevelsError(refExpr.getPLevel(), refExpr.getHLevel()));
    }

    boolean ok = checkArguments(contextData.getArguments(), errorReporter, contextData.getReferenceExpression());

    if (contextData.getExpectedType() == null && requireExpectedType()) {
      errorReporter.report(new TypecheckingError("Cannot infer the expected type", contextData.getReferenceExpression()));
      ok = false;
    }

    return ok;
  }
}
