package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.*;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.error.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ContextDataChecker {
  /**
   * @return  true if this meta definition does not support level annotations
   */
  public boolean withoutLevels() {
    return true;
  }

  /**
   * @return  an array representing the expected number of arguments and their explicitness; returns {@code null} if the definition can accept any number of arguments without restrictions
   */
  public boolean @Nullable [] argumentExplicitness() {
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

  public boolean allowCoclauses() {
    return false;
  }

  public boolean allowEmptyCoclauses() {
    return allowCoclauses();
  }

  public boolean allowClauses() {
    return false;
  }

  protected boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments, ErrorReporter errorReporter, ConcreteExpression marker, boolean[] explicitness) {
    boolean ok = true;
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
            errorReporter.report(new MissingArgumentsError(diff, marker));
          }
          ok = false;
        }
      }
    }
    return ok;
  }

  protected boolean checkLevels(ConcreteReferenceExpression refExpr, ErrorReporter errorReporter) {
    if (withoutLevels() && (refExpr.getPLevel() != null || refExpr.getHLevel() != null)) {
      errorReporter.report(new IgnoredLevelsError(refExpr.getPLevel(), refExpr.getHLevel()));
      return false;
    } else {
      return true;
    }
  }

  protected boolean checkCoclauses(ConcreteCoclauses coclauses, ErrorReporter errorReporter) {
    if (coclauses != null && !(coclauses.getCoclauseList().isEmpty() && allowEmptyCoclauses() || !coclauses.getCoclauseList().isEmpty() && allowCoclauses())) {
      errorReporter.report(new TypecheckingError("Coclauses are not allowed here", coclauses));
      return false;
    } else {
      return true;
    }
  }

  protected boolean checkClauses(ConcreteClauses clauses, ErrorReporter errorReporter) {
    if (clauses != null && !allowClauses()) {
      errorReporter.report(new TypecheckingError("Clauses are not allowed here", clauses));
      return false;
    } else {
      return true;
    }
  }

  protected boolean checkExpectedType(CoreExpression expectedType, ConcreteReferenceExpression refExpr, ErrorReporter errorReporter) {
    if (expectedType == null && requireExpectedType()) {
      errorReporter.report(new TypecheckingError("Cannot infer the expected type", refExpr));
      return false;
    } else {
      return true;
    }
  }

  public boolean checkContextData(@NotNull ContextData contextData, @NotNull ErrorReporter errorReporter) {
    ConcreteReferenceExpression refExpr = contextData.getReferenceExpression();
    checkLevels(refExpr, errorReporter);
    boolean ok = checkArguments(contextData.getArguments(), errorReporter, refExpr, argumentExplicitness());
    ok = checkCoclauses(contextData.getCoclauses(), errorReporter) && ok;
    ok = checkClauses(contextData.getClauses(), errorReporter) && ok;
    ok = checkExpectedType(contextData.getExpectedType(), refExpr, errorReporter) && ok;
    return ok;
  }
}
