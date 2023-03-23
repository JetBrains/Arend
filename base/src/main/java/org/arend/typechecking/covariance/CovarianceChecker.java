package org.arend.typechecking.covariance;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.prelude.Prelude;

import java.util.Map;

public abstract class CovarianceChecker {
  protected boolean allowData() {
    return true;
  }

  protected abstract boolean checkNonCovariant(Expression expr);

  protected boolean checkOtherwise(Expression expr) {
    return checkNonCovariant(expr);
  }

  protected boolean checkLevels(Levels levels, DefCallExpression defCall) {
    return false;
  }

  private boolean checkConstructor(Expression expr) {
    expr = expr.getUnderlyingExpression();

    if (expr instanceof LamExpression) {
      return checkConstructor(((LamExpression) expr).getBody());
    }

    if (expr instanceof TupleExpression) {
      for (Expression field : ((TupleExpression) expr).getFields()) {
        if (checkConstructor(field)) {
          return true;
        }
      }
      return false;
    }

    if (expr instanceof ConCallExpression) {
      for (Expression argument : ((ConCallExpression) expr).getDefCallArguments()) {
        if (checkConstructor(argument)) {
          return true;
        }
      }
      return false;
    }

    if (expr instanceof NewExpression newExpr) {
      if (newExpr.getRenewExpression() != null) {
        return checkNonCovariant(newExpr.getRenewExpression());
      }

      for (Expression expression : newExpr.getClassCall().getImplementedHere().values()) {
        if (checkConstructor(expression)) {
          return true;
        }
      }

      return false;
    }

    return check(expr);
  }

  public boolean check(Expression expr) {
    if (expr == null) {
      return false;
    }
    expr = expr.getUnderlyingExpression();

    if (expr instanceof UniverseExpression) {
      Sort sort = ((UniverseExpression) expr).getSort();
      return checkLevels(new LevelPair(sort.getPLevel(), sort.getHLevel()), null);
    }

    if (expr instanceof PiExpression) {
      for (DependentLink link = ((PiExpression) expr).getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (checkNonCovariant(link.getTypeExpr())) {
          return true;
        }
      }
      return check(((PiExpression) expr).getCodomain());
    }

    if (expr instanceof SigmaExpression) {
      for (DependentLink link = ((SigmaExpression) expr).getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (check(link.getTypeExpr())) {
          return true;
        }
      }
      return false;
    }

    if (expr instanceof DataCallExpression dataCall && allowData()) {
      if (checkLevels(dataCall.getLevels(), dataCall)) {
        return true;
      }
      int i = 0;
      for (Expression argument : dataCall.getDefCallArguments()) {
        if (dataCall.getDefinition().isCovariant(i)) {
          if (checkConstructor(argument)) {
            return true;
          }
        } else {
          if (checkNonCovariant(argument)) {
            return true;
          }
        }
        i++;
      }
      return false;
    }

    if (expr instanceof ClassCallExpression classCall) {
      if (checkLevels(classCall.getLevels(), classCall)) {
        return true;
      }
      for (Map.Entry<ClassField, Expression> entry : classCall.getImplementedHere().entrySet()) {
        if (classCall.getDefinition().isCovariantField(entry.getKey())) {
          if (checkConstructor(entry.getValue())) {
            return true;
          }
        } else {
          if (checkNonCovariant(entry.getValue())) {
            return true;
          }
        }
      }
      return false;
    }

    if (expr instanceof FunCallExpression funCall && funCall.getDefinition() == Prelude.PATH_INFIX && allowData()) {
      if (checkLevels(funCall.getLevels(), funCall)) {
        return true;
      }
      if (check(funCall.getDefCallArguments().get(0))) {
        return true;
      }
      return checkNonCovariant(funCall.getDefCallArguments().get(1)) || checkNonCovariant(funCall.getDefCallArguments().get(2));
    }

    if (expr instanceof FunCallExpression funCall && funCall.getDefinition() == Prelude.ARRAY) {
      return checkLevels(funCall.getLevels(), funCall) || check(funCall.getDefCallArguments().get(0));
    }

    return checkOtherwise(expr);
  }
}
