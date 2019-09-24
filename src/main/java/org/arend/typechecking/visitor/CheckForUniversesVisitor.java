package org.arend.typechecking.visitor;

import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.*;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Sort;
import org.arend.util.Pair;

import java.util.Map;

public class CheckForUniversesVisitor extends ProcessDefCallsVisitor<Void> {
  private boolean myCheckTopLevel;

  public CheckForUniversesVisitor(boolean checkTopLevel) {
    myCheckTopLevel = checkTopLevel;
  }

  public static boolean findUniverse(Expression expr) {
    return expr != null && expr.accept(new CheckForUniversesVisitor(true), null);
  }

  public static boolean findUniverse(Body body) {
    if (body instanceof IntervalElim) {
      for (Pair<Expression, Expression> pair : ((IntervalElim) body).getCases()) {
        if (findUniverse(pair.proj1) || findUniverse(pair.proj2)) {
          return true;
        }
      }
      body = ((IntervalElim) body).getOtherwise();
    }

    if (body instanceof Expression) {
      return findUniverse((Expression) body);
    } else if (body instanceof ElimBody) {
      for (ElimClause clause : ((ElimBody) body).getClauses()) {
        if (findUniverse(clause.expression)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean visitSort(Sort sort) {
    return !sort.getPLevel().isClosed() || !sort.getHLevel().isClosed();
  }

  @Override
  public boolean processDefCall(DefCallExpression expression, Void param) {
    if (!myCheckTopLevel) {
      myCheckTopLevel = true;
      return false;
    }
    if (expression.getDefinition() instanceof ClassField) {
      return false;
    }
    return expression.hasUniverses() && visitSort(expression.getSortArgument());
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    if (!myCheckTopLevel) {
      myCheckTopLevel = true;
      return false;
    }
    return visitSort(expression.getSort());
  }
}
