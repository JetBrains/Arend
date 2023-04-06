package org.arend.typechecking.visitor;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.naming.reference.TCDefReferable;

import java.util.Set;

public class CheckForUniversesVisitor extends SearchVisitor<Void> {
  private final Set<? extends TCDefReferable> myRecursiveDefinitions;

  public CheckForUniversesVisitor(Set<? extends TCDefReferable> recursiveDefinitions) {
    myRecursiveDefinitions = recursiveDefinitions;
  }

  @Override
  public CoreExpression.FindAction processDefCall(DefCallExpression expression, Void param) {
    if (expression.getDefinition() instanceof ClassField) {
      return CoreExpression.FindAction.CONTINUE;
    }
    assert expression instanceof LeveledDefCallExpression;
    return expression.getUniverseKind() != UniverseKind.NO_UNIVERSES && !((LeveledDefCallExpression) expression).getLevels().isClosed() ? CoreExpression.FindAction.STOP : CoreExpression.FindAction.CONTINUE;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    return !expression.getSort().getPLevel().isClosed() || !expression.getSort().getHLevel().isClosed();
  }

  private boolean visitFieldCall(FieldCallExpression expr, int apps) {
    if (expr.getDefinition().isProperty()) {
      return false;
    }
    Expression arg = expr.getArgument();
    if (arg instanceof FunCallExpression funCall && funCall.getDefinition().getResultType() instanceof ClassCallExpression && funCall.getDefinition().status().isOK() && !myRecursiveDefinitions.contains(funCall.getDefinition().getRef())) {
      Expression result = NormalizeVisitor.INSTANCE.evalFieldCall(expr.getDefinition(), arg);
      if (result != null) {
        while (apps > 0 && result instanceof LamExpression lam) {
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
  public Boolean visitTypeConstructor(TypeConstructorExpression expr, Void params) {
    return expr.getDefinition().getUniverseKind() != UniverseKind.NO_UNIVERSES && !expr.getLevels().isClosed();
  }

  @Override
  public Boolean visitNew(NewExpression expression, Void param) {
    if (expression.getRenewExpression() != null && expression.getRenewExpression().accept(this, param)) {
      return true;
    }
    for (Expression impl : expression.getClassCall().getImplementedHere().values()) {
      if (impl.accept(this, param)) {
        return true;
      }
    }
    return false;
  }
}
