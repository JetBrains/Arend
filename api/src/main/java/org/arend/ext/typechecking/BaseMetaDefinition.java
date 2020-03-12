package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.IgnoredLevelsError;
import org.arend.ext.error.TypecheckingError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BaseMetaDefinition implements MetaDefinition {
  protected boolean withoutLevels() {
    return false;
  }

  @Nullable
  protected boolean[] argumentExplicitness() {
    return null;
  }

  protected boolean requireExpectedType() {
    return false;
  }

  protected int numberOfOptionalExplicitArguments() {
    return 0;
  }

  protected boolean allowExcessiveArguments() {
    return !requireExpectedType();
  }

  protected boolean checkContextData(@NotNull ContextData contextData, @NotNull ErrorReporter errorReporter) {
    ConcreteReferenceExpression refExpr = contextData.getReferenceExpression();
    if (withoutLevels() && (refExpr.getPLevel() != null || refExpr.getHLevel() != null)) {
      errorReporter.report(new IgnoredLevelsError(refExpr.getPLevel(), refExpr.getHLevel()));
    }

    boolean ok = true;
    boolean[] explicitness = argumentExplicitness();
    if (explicitness != null) {
      List<? extends ConcreteArgument> args = contextData.getArguments();
      int i = 0, j = 0;
      while (i < explicitness.length && j < args.size()) {
        if (explicitness[i] == args.get(j).isExplicit()) {
          i++;
          j++;
        } else if (explicitness[i]) {
          errorReporter.report(new ArgumentExplicitnessError(true, args.get(j).getExpression()));
          j++;
        } else {
          i++;
        }
      }

      if (j < args.size() && !allowExcessiveArguments()) {
        errorReporter.report(new TypecheckingError("Excessive arguments are not allowed for '" + refExpr.getReferent().getRefName() + "'", args.get(j).getExpression()));
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
          errorReporter.report(new TypecheckingError("Required " + diff + " more explicit argument" + (diff == 1 ? "" : "s"), refExpr));
          ok = false;
        }
      }
    }

    if (contextData.getExpectedType() == null && requireExpectedType()) {
      errorReporter.report(new TypecheckingError("Cannot infer the expected type", refExpr));
      ok = false;
    }
    return ok;
  }
}
