package org.arend.typechecking.covariance;

import org.arend.core.context.binding.Variable;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.FindBindingVisitor;
import org.arend.prelude.Prelude;

import java.util.Collections;
import java.util.List;

// TODO[lang_ext]: Calculate the set of covariant fields and parameters at once.
public class ParametersCovarianceChecker extends CovarianceChecker {
  private final FindBindingVisitor myVisitor;

  public ParametersCovarianceChecker(Variable variable) {
    myVisitor = new FindBindingVisitor(Collections.singleton(variable));
  }

  @Override
  protected boolean checkNonCovariant(Expression expr) {
    while (true) {
      if (expr instanceof AppExpression) {
        if (((AppExpression) expr).getArgument().accept(myVisitor, null) != null) {
          return true;
        }
        expr = expr.getFunction();
      } else if (expr instanceof ProjExpression) {
        expr = ((ProjExpression) expr).getExpression();
      } else if (expr instanceof FieldCallExpression) {
        expr = ((FieldCallExpression) expr).getArgument();
      } else if (expr instanceof FunCallExpression && ((FunCallExpression) expr).getDefinition() == Prelude.AT) {
        List<? extends Expression> args = ((FunCallExpression) expr).getDefCallArguments();
        for (int i = 0; i < args.size(); i++) {
          if (i != 3 && args.get(i).accept(myVisitor, null) != null) {
            return true;
          }
        }
        expr = args.get(3);
      } else if (expr instanceof ReferenceExpression) {
        return false;
      } else {
        return expr.accept(myVisitor, null) != null;
      }
    }
  }
}
