package org.arend.core.expr.visitor;

import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.level.LevelSubstitution;

public class FieldCallSubstVisitor extends SubstVisitor {
  private final ClassCallExpression myClassCall;
  private final Expression myThisExpr;
  private boolean myAllReplaced = true;

  public FieldCallSubstVisitor(ClassCallExpression classCall, Expression thisExpr) {
    super(new ExprSubstitution(), LevelSubstitution.EMPTY);
    myClassCall = classCall;
    myThisExpr = thisExpr;
  }

  public boolean areAllReplaced() {
    return myAllReplaced;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    ReferenceExpression refExpr = expr.getArgument().cast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding() == myClassCall.getThisBinding()) {
      Expression impl = myClassCall.getImplementation(expr.getDefinition(), myThisExpr);
      if (impl != null) {
        return impl;
      }
      myAllReplaced = false;
    }
    return super.visitFieldCall(expr, params);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (expr.getBinding() == myClassCall.getThisBinding()) {
      myAllReplaced = false;
    }
    return super.visitReference(expr, params);
  }
}
