package org.arend.typechecking.covariance;

import org.arend.core.definition.Definition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.elimtree.*;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Sort;
import org.arend.typechecking.visitor.CheckForUniversesVisitor;
import org.arend.util.Pair;

public class UniverseKindChecker extends CovarianceChecker {
  private final CheckForUniversesVisitor myVisitor = new CheckForUniversesVisitor();
  private UniverseKind myResult = UniverseKind.NO_UNIVERSES;

  public UniverseKind getUniverseKind(Expression expression) {
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
  protected boolean checkSort(Sort sort, Definition definition) {
    if (!CheckForUniversesVisitor.visitSort(sort)) {
      return false;
    }

    myResult = myResult.max(definition == null ? UniverseKind.ONLY_COVARIANT : definition.getUniverseKind());
    return myResult == UniverseKind.WITH_UNIVERSES;
  }
}
