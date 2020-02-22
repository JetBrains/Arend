package org.arend.typechecking.subexpr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

import java.util.List;
import java.util.function.Function;

public class FindBinding {
  public static DependentLink visitLam(
      Referable referable,
      Concrete.LamExpression expr,
      LamExpression lam
  ) {
    return parameters(lam.getParameters(), referable, new Function<DependentLink, DependentLink>() {
      private LamExpression lambda = lam;

      @Override
      public DependentLink apply(DependentLink link) {
        DependentLink next = link.getNext();
        if (next instanceof EmptyDependentLink) {
          lambda = lambda.getBody().cast(LamExpression.class);
          return lambda.getParameters();
        } else return next;
      }
    }, expr.getParameters());
  }

  public static DependentLink visitPi(
      Referable referable,
      Concrete.PiExpression expr,
      PiExpression pi
  ) {
    return parameters(pi.getBinding(), referable, new Function<DependentLink, DependentLink>() {
      private PiExpression piExpr = pi;

      @Override
      public DependentLink apply(DependentLink link) {
        DependentLink next = link.getNext();
        if (next instanceof EmptyDependentLink) {
          piExpr = piExpr.getCodomain().cast(PiExpression.class);
          return piExpr.getBinding();
        } else return next;
      }
    }, expr.getParameters());
  }

  public static DependentLink visitSigma(
      Referable referable,
      Concrete.SigmaExpression expr,
      SigmaExpression sigma
  ) {
    return parameters(sigma.getParameters(), referable, DependentLink::getNext, expr.getParameters());
  }

  public static Expression visitLet(
      Concrete.LetClausePattern pattern,
      Concrete.LetExpression expr,
      LetExpression let
  ) {
    List<Concrete.LetClause> exprClauses = expr.getClauses();
    List<LetClause> coreClauses = let.getClauses();
    for (int i = 0; i < exprClauses.size(); i++) {
      LetClause coreLetClause = coreClauses.get(i);
      Concrete.LetClause exprLetClause = exprClauses.get(i);

      if (exprLetClause.getPattern() == pattern)
        return coreLetClause.getTypeExpr();
    }
    return null;
  }

  private static DependentLink parameters(
      DependentLink core,
      Referable referable,
      Function<DependentLink, DependentLink> next,
      List<? extends Concrete.Parameter> parameters
  ) {
    for (Concrete.Parameter concrete : parameters)
      for (Referable ref : concrete.getReferableList()) {
        if (ref == referable) return core;
        if (concrete.isExplicit() != core.isExplicit()) continue;
        core = next.apply(core);
      }
    return null;
  }
}
