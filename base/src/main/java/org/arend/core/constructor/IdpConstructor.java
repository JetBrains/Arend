package org.arend.core.constructor;

import org.arend.core.context.param.UnusedIntervalDependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizingFindBindingVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;

import java.util.Collections;
import java.util.List;

public class IdpConstructor extends SingleConstructor {
  @Override
  public List<Expression> getMatchedArguments(Expression argument, boolean normalizing) {
    argument = argument.getUnderlyingExpression();
    if (argument instanceof FunCallExpression) {
      return ((FunCallExpression) argument).getDefinition() == Prelude.IDP ? Collections.emptyList() : null;
    }

    if (!normalizing || !(argument instanceof PathExpression)) {
      return null;
    }

    LamExpression lamExpr = ((PathExpression) argument).getArgument().normalize(NormalizationMode.WHNF).cast(LamExpression.class);
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
