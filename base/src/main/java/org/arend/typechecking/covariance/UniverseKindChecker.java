package org.arend.typechecking.covariance;

import org.arend.core.definition.UniverseKind;
import org.arend.core.elimtree.*;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.subst.Levels;
import org.arend.ext.util.Pair;
import org.arend.naming.reference.TCDefReferable;
import org.arend.typechecking.visitor.CheckForUniversesVisitor;

import java.util.Set;

public class UniverseKindChecker extends CovarianceChecker {
  private final CheckForUniversesVisitor myVisitor;
  private UniverseKind myResult = UniverseKind.NO_UNIVERSES;

  public UniverseKindChecker(Set<? extends TCDefReferable> recursiveDefinitions) {
    myVisitor = new CheckForUniversesVisitor(recursiveDefinitions);
  }

  public UniverseKind getUniverseKind(Expression expression) {
    myResult = UniverseKind.NO_UNIVERSES;
    check(expression);
    return myResult;
  }

  public UniverseKind getUniverseKind(Body body) {
    if (body instanceof Expression) {
      check((Expression) body);
    } else if (body instanceof IntervalElim) {
      if (getUniverseKind(((IntervalElim) body).getOtherwise()) == UniverseKind.WITH_UNIVERSES) {
        return myResult;
      }
      for (Pair<Expression, Expression> pair : ((IntervalElim) body).getCases()) {
        if (pair.proj1 != null && getUniverseKind(pair.proj1) == UniverseKind.WITH_UNIVERSES) {
          return myResult;
        }
        if (pair.proj2 != null && getUniverseKind(pair.proj2) == UniverseKind.WITH_UNIVERSES) {
          return myResult;
        }
      }
    } else if (body instanceof ElimBody) {
      for (var clause : ((ElimBody) body).getClauses()) {
        if (clause.getExpression() != null) {
          check(clause.getExpression());
        }
      }
    } else if (body != null) {
      throw new IllegalStateException();
    }

    return myResult;
  }

  @Override
  protected boolean checkNonCovariant(Expression expr) {
    if (expr.accept(myVisitor, null)) {
      myResult = UniverseKind.WITH_UNIVERSES;
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected boolean checkLevels(Levels levels, DefCallExpression defCall) {
    if (levels.isClosed()) {
      return false;
    }

    myResult = myResult.max(defCall == null ? UniverseKind.ONLY_COVARIANT : defCall.getUniverseKind());
    return myResult == UniverseKind.WITH_UNIVERSES;
  }
}
