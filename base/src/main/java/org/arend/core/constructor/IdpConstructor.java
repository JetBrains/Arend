package org.arend.core.constructor;

import org.arend.core.context.param.UnusedIntervalDependentLink;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FunCallExpression;
import org.arend.core.expr.LamExpression;
import org.arend.core.expr.visitor.NormalizingFindBindingVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;

import java.util.Collections;
import java.util.List;

public class IdpConstructor extends SingleConstructor {
  @Override
  public int getNumberOfParameters() {
    return 0;
  }

  @Override
  public List<Expression> getMatchedArguments(Expression argument, boolean normalizing) {
    argument = argument.getUnderlyingExpression();
    if (argument instanceof FunCallExpression) {
      return ((FunCallExpression) argument).getDefinition() == Prelude.IDP ? Collections.emptyList() : null;
    }

    if (!normalizing || !(argument instanceof ConCallExpression && ((ConCallExpression) argument).getDefinition() == Prelude.PATH_CON)) {
      return null;
    }

    LamExpression lamExpr = ((ConCallExpression) argument).getDefCallArguments().get(0).normalize(NormalizationMode.WHNF).cast(LamExpression.class);
    if (lamExpr == null) {
      return null;
    }
    Expression body = lamExpr.getParameters().getNext().hasNext() ? new LamExpression(lamExpr.getResultSort(), lamExpr.getParameters().getNext(), lamExpr.getBody()) : lamExpr.getBody();
    return lamExpr.getParameters() == UnusedIntervalDependentLink.INSTANCE || !NormalizingFindBindingVisitor.findBinding(body, lamExpr.getParameters()) ? Collections.emptyList() : null;
  }

  @Override
  public boolean compare(SingleConstructor other, Equations equations, Concrete.SourceNode sourceNode) {
    return other instanceof IdpConstructor;
  }
}
