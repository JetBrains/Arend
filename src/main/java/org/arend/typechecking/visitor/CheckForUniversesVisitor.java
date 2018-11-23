package org.arend.typechecking.visitor;

import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.*;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Sort;
import org.arend.util.Pair;

import java.util.Map;

public class CheckForUniversesVisitor extends ProcessDefCallsVisitor<Void> {
  public static boolean findUniverse(Expression expr) {
    return expr != null && expr.accept(new CheckForUniversesVisitor(), null);
  }

  public static boolean findUniverse(Body body) {
    if (body instanceof IntervalElim) {
      for (Pair<Expression, Expression> pair : ((IntervalElim) body).getCases()) {
        if (findUniverse(pair.proj1) || findUniverse(pair.proj2)) {
          return true;
        }
      }
      return findUniverse(((IntervalElim) body).getOtherwise());
    } else if (body instanceof LeafElimTree) {
      return findUniverse(((LeafElimTree) body).getExpression());
    } else if (body instanceof BranchElimTree) {
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) body).getChildren()) {
        if (findUniverse(entry.getValue())) {
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
  public Boolean visitDefCall(DefCallExpression expression, Void param) {
    return expression.getDefinition().hasUniverses() && visitSort(expression.getSortArgument());
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    return visitSort(expression.getSort());
  }
}
