package org.arend.typechecking.visitor;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Level;
import org.arend.ext.core.expr.CoreExpression;

public class CheckForUniversesVisitor extends SearchVisitor<Void> {
  public static boolean visitLevels(Level pLevel, Level hLevel) {
    return !pLevel.isClosed() || !hLevel.isClosed();
  }

  @Override
  public CoreExpression.FindAction processDefCall(DefCallExpression expression, Void param) {
    if (expression.getDefinition() instanceof ClassField) {
      return CoreExpression.FindAction.CONTINUE;
    }
    return expression.getUniverseKind() != UniverseKind.NO_UNIVERSES && visitLevels(expression.getPLevel(), expression.getHLevel()) ? CoreExpression.FindAction.STOP : CoreExpression.FindAction.CONTINUE;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    return visitLevels(expression.getSort().getPLevel(), expression.getSort().getHLevel());
  }

  private boolean visitFieldCall(FieldCallExpression expr, int apps) {
    if (expr.getDefinition().isProperty()) {
      return false;
    }
    Expression arg = expr.getArgument();
    if (arg instanceof FunCallExpression && ((FunCallExpression) arg).getDefinition().getResultType() instanceof ClassCallExpression && ((FunCallExpression) arg).getDefinition().status().isOK()) {
      Expression result = NormalizeVisitor.INSTANCE.evalFieldCall(expr.getDefinition(), arg);
      if (result != null) {
        while (apps > 0 && result instanceof LamExpression) {
          LamExpression lam = (LamExpression) result;
          SingleDependentLink param = lam.getParameters();
          for (; param.hasNext() && apps > 0; param = param.getNext()) {
            apps--;
          }
          result = param.hasNext() ? new LamExpression(lam.getResultSort(), param, lam.getBody()) : lam.getBody();
        }
        return result.accept(this, null);
      }
    }
    return arg.accept(this, null);
  }

  @Override
  public Boolean visitFieldCall(FieldCallExpression expr, Void params) {
    return visitFieldCall(expr, 0);
  }

  @Override
  public Boolean visitApp(AppExpression expression, Void param) {
    int apps = 0;
    Expression expr = expression;
    while (expr instanceof AppExpression) {
      if (((AppExpression) expr).getArgument().accept(this, null)) {
        return true;
      }
      expr = expr.getFunction();
      apps++;
    }
    return expr instanceof FieldCallExpression ? visitFieldCall((FieldCallExpression) expr, apps) : expr.accept(this, null);
  }

  @Override
  public Boolean visitTypeCoerce(TypeCoerceExpression expr, Void params) {
    return expr.getDefinition().getUniverseKind() != UniverseKind.NO_UNIVERSES && visitLevels(expr.getLevels().get(LevelVariable.PVAR), expr.getLevels().get(LevelVariable.HVAR));
  }
}
