package org.arend.typechecking.covariance;

import org.arend.ext.variable.Variable;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.VoidExpressionVisitor;

import java.util.Set;

public class ParametersCovarianceChecker extends CovarianceChecker {
  private final Set<? extends Variable> myVariables;
  private final VoidExpressionVisitor<Void> myVisitor;

  public ParametersCovarianceChecker(Set<? extends Variable> variables) {
    myVariables = variables;

    myVisitor = new VoidExpressionVisitor<>() {
      @Override
      public Void visitReference(ReferenceExpression expr, Void params) {
        myVariables.remove(expr.getBinding());
        return null;
      }

      @Override
      public Void visitFieldCall(FieldCallExpression expr, Void params) {
        myVariables.remove(expr.getDefinition());
        return null;
      }
    };
  }

  @Override
  protected boolean checkNonCovariant(Expression expr) {
    expr.accept(myVisitor, null);
    return myVariables.isEmpty();
  }

  public boolean checkNonCovariant(Body body) {
    myVisitor.visitBody(body, null);
    return myVariables.isEmpty();
  }

  @Override
  protected boolean checkOtherwise(Expression expr) {
    while (true) {
      if (expr instanceof AppExpression) {
        if (checkNonCovariant(((AppExpression) expr).getArgument())) {
          return true;
        }
        expr = expr.getFunction();
      } else if (expr instanceof ProjExpression) {
        expr = ((ProjExpression) expr).getExpression();
      } else if (expr instanceof FieldCallExpression) {
        expr = ((FieldCallExpression) expr).getArgument();
      } else if (expr instanceof AtExpression) {
        if (checkNonCovariant(((AtExpression) expr).getIntervalArgument())) {
          return true;
        }
        expr = ((AtExpression) expr).getPathArgument();
      } else if (expr instanceof ReferenceExpression) {
        return false;
      } else {
        return checkNonCovariant(expr);
      }
    }
  }
}
