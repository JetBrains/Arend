package org.arend.typechecking.visitor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.visitor.BaseExpressionVisitor;

public abstract class SearchVisitor<P> extends BaseExpressionVisitor<P, Boolean> {
  protected boolean processDefCall(DefCallExpression expression, P param) {
    return false;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expression, P param) {
    return processDefCall(expression, param) || expression.getDefCallArguments().stream().anyMatch(arg -> arg.accept(this, param));
  }

  protected boolean visitConCallArgument(Expression arg, P param) {
    return arg.accept(this, param);
  }

  @Override
  public Boolean visitConCall(ConCallExpression expression, P param) {
    Expression it = expression;
    do {
      expression = (ConCallExpression) it;
      if (processDefCall(expression, param)) {
        return true;
      }

      for (Expression arg : expression.getDataTypeArguments()) {
        if (visitConCallArgument(arg, param)) {
          return true;
        }
      }

      int recursiveParam = expression.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        for (Expression arg : expression.getDefCallArguments()) {
          if (visitConCallArgument(arg, param)) {
            return true;
          }
        }
        return false;
      }

      for (int i = 0; i < expression.getDefCallArguments().size(); i++) {
        if (i != recursiveParam && visitConCallArgument(expression.getDefCallArguments().get(i), param)) {
          return true;
        }
      }

      it = expression.getDefCallArguments().get(recursiveParam);
    } while (it instanceof ConCallExpression);

    return visitConCallArgument(it, param);
  }

  @Override
  public Boolean visitApp(AppExpression expression, P param) {
    return expression.getFunction().accept(this, param) || expression.getArgument().accept(this, param);
  }

  @Override
  public Boolean visitLet(LetExpression expression, P param) {
    if (expression.getExpression().accept(this, param)) {
      return true;
    }

    for (LetClause lc : expression.getClauses()) {
      if (lc.getExpression().accept(this, param)) {
        return true;
      }
    }
    return false;
  }

  protected boolean visitElimBody(ElimBody elimBody, P param) {
    for (var clause : elimBody.getClauses()) {
      if (visitDependentLink(clause.getParameters(), null) || clause.getExpression() != null && clause.getExpression().accept(this, param)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitCase(CaseExpression expr, P param) {
    return visitElimBody(expr.getElimBody(), param) || visitDependentLink(expr.getParameters(), param) || expr.getResultType().accept(this, param) || expr.getResultTypeLevel() != null && expr.getResultTypeLevel().accept(this, param) || expr.getArguments().stream().anyMatch(arg -> arg.accept(this, param));
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expression, P param) {
    return visitDefCall(expression, param) || expression.getImplementedHere().values().stream().anyMatch(expr -> expr.accept(this, param));
  }

  protected boolean visitDependentLink(DependentLink link, P param) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      if (link.getTypeExpr().accept(this, param)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitLam(LamExpression expression, P param) {
    return visitDependentLink(expression.getParameters(), param) || expression.getBody().accept(this, param);
  }

  @Override
  public Boolean visitPi(PiExpression expression, P param) {
    return visitDependentLink(expression.getParameters(), param) || expression.getCodomain().accept(this, param);
  }

  @Override
  public Boolean visitSigma(SigmaExpression expression, P param) {
    return visitDependentLink(expression.getParameters(), param);
  }

  @Override
  public Boolean visitTuple(TupleExpression expression, P param) {
    return expression.getFields().stream().anyMatch(e -> e.accept(this, param)) || expression.getSigmaType().accept(this, param);
  }

  @Override
  public Boolean visitProj(ProjExpression expression, P param) {
    return expression.getExpression().accept(this, param);
  }

  @Override
  public Boolean visitNew(NewExpression expression, P param) {
    return expression.getClassCall().accept(this, param) || expression.getRenewExpression() != null && expression.getRenewExpression().accept(this, param);
  }

  @Override
  public Boolean visitPEval(PEvalExpression expr, P param) {
    return expr.getExpression().accept(this, param);
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, P param) {
    return false;
  }

  @Override
  public Boolean visitError(ErrorExpression expression, P param) {
    return false;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expression, P param) {
    return false;
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expression, P param) {
    return expression.getSubstExpression() != null && expression.getSubstExpression().accept(this, param);
  }

  @Override
  public Boolean visitSubst(SubstExpression expr, P param) {
    return expr.getSubstExpression().accept(this, param);
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expression, P param) {
    return expression.getExpression().accept(this, param) || expression.getTypeOf().accept(this, param);
  }

  @Override
  public Boolean visitInteger(IntegerExpression expr, P params) {
    return false;
  }
}
