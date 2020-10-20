package org.arend.typechecking.covariance;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
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

  protected boolean checkSort(Sort sort, DefCallExpression defCall) {
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

    if (expr instanceof NewExpression) {
      NewExpression newExpr = (NewExpression) expr;
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
      return checkSort(((UniverseExpression) expr).getSort(), null);
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

    if (expr instanceof DataCallExpression && (allowData() || ((DataCallExpression) expr).getDefinition() == Prelude.PATH)) {
      DataCallExpression dataCall = (DataCallExpression) expr;
      if (checkSort(dataCall.getSortArgument(), dataCall)) {
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

    if (expr instanceof ClassCallExpression) {
      ClassCallExpression classCall = (ClassCallExpression) expr;
      if (checkSort(classCall.getSortArgument(), classCall)) {
        return true;
      }
      for (Map.Entry<ClassField, Expression> entry : classCall.getImplementedHere().entrySet()) {
        if (entry.getKey().isCovariant()) {
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

    if (expr instanceof FunCallExpression && ((FunCallExpression) expr).getDefinition() == Prelude.PATH_INFIX) {
      FunCallExpression funCall = (FunCallExpression) expr;
      if (checkSort(funCall.getSortArgument(), funCall)) {
        return true;
      }
      if (check(funCall.getDefCallArguments().get(0))) {
        return true;
      }
      return checkNonCovariant(funCall.getDefCallArguments().get(1)) || checkNonCovariant(funCall.getDefCallArguments().get(2));
    }

    return checkOtherwise(expr);
  }
}
