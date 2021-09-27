package org.arend.typechecking.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.visitor.BaseExpressionVisitor;
import org.arend.ext.core.expr.CoreExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class SearchVisitor<P> extends BaseExpressionVisitor<P, Boolean> {
  protected CoreExpression.FindAction processDefCall(DefCallExpression expression, P param) {
    return CoreExpression.FindAction.CONTINUE;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expression, P param) {
    switch (processDefCall(expression, param)) {
      case STOP: return true;
      case SKIP: return false;
      default: return expression.getDefCallArguments().stream().anyMatch(arg -> arg.accept(this, param));
    }
  }

  protected boolean checkPathArgumentType() {
    return true;
  }

  protected boolean preserveOrder() {
    return false;
  }

  protected boolean visitConCallArgument(Expression arg, P param) {
    return arg.accept(this, param);
  }

  @Override
  public Boolean visitConCall(ConCallExpression expression, P param) {
    if (preserveOrder() && expression.getDefinition().getRecursiveParameter() >= 0 && expression.getDefinition().getRecursiveParameter() < expression.getDefCallArguments().size() - 1) {
      List<ConCallExpression> stack = new ArrayList<>();
      while (true) {
        switch (processDefCall(expression, param)) {
          case STOP: return true;
          case SKIP: break;
        }

        for (Expression arg : expression.getDataTypeArguments()) {
          if (visitConCallArgument(arg, param)) {
            return true;
          }
        }

        for (int i = 0; i < expression.getDefinition().getRecursiveParameter(); i++) {
          if (visitConCallArgument(expression.getDefCallArguments().get(i), param)) {
            return true;
          }
        }

        stack.add(expression);

        Expression rec = expression.getDefCallArguments().get(expression.getDefinition().getRecursiveParameter());
        if (!(rec instanceof ConCallExpression)) {
          if (visitConCallArgument(rec, param)) {
            return true;
          }
          break;
        }

        expression = (ConCallExpression) rec;
      }

      for (int i = stack.size() - 1; i >= 0; i--) {
        expression = stack.get(i);
        for (int j = expression.getDefinition().getRecursiveParameter() + 1; j < expression.getDefCallArguments().size(); j++) {
          if (visitConCallArgument(expression.getDefCallArguments().get(j), param)) {
            return true;
          }
        }
      }

      return false;
    }

    Expression it = expression;
    do {
      expression = (ConCallExpression) it;
      switch (processDefCall(expression, param)) {
        case STOP: return true;
        case SKIP: return false;
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
  public Boolean visitPath(PathExpression expr, P params) {
    return checkPathArgumentType() && expr.getArgumentType() != null && expr.getArgumentType().accept(this, params) || expr.getArgument().accept(this, params);
  }

  @Override
  public Boolean visitApp(AppExpression expression, P param) {
    return expression.getFunction().accept(this, param) || expression.getArgument().accept(this, param);
  }

  @Override
  public Boolean visitLet(LetExpression expression, P param) {
    for (HaveClause lc : expression.getClauses()) {
      if (lc.getExpression().accept(this, param)) {
        return true;
      }
    }

    return expression.getExpression().accept(this, param);
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
    return expr.getArguments().stream().anyMatch(arg -> arg.accept(this, param)) || visitDependentLink(expr.getParameters(), param) || expr.getResultType().accept(this, param) || expr.getResultTypeLevel() != null && expr.getResultTypeLevel().accept(this, param) || visitElimBody(expr.getElimBody(), param);
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expression, P param) {
    switch (processDefCall(expression, param)) {
      case STOP: return true;
      case SKIP: return false;
    }

    if (preserveOrder()) {
      if (expression.getImplementedHere().isEmpty()) {
        return false;
      }
      for (ClassField field : expression.getDefinition().getFields()) {
        Expression impl = expression.getAbsImplementationHere(field);
        if (impl != null && impl.accept(this, param)) {
          return true;
        }
      }
    } else {
      for (Expression impl : expression.getImplementedHere().values()) {
        if (impl.accept(this, param)) {
          return true;
        }
      }
    }

    return false;
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
    return expression.getRenewExpression() != null && expression.getRenewExpression().accept(this, param) || visitClassCall(expression.getClassCall(), param);
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
    if (expr.isInferenceVariable()) {
      if (expr.getExpression().accept(this, param)) {
        return true;
      }
      for (Map.Entry<Binding, Expression> entry : expr.getSubstitution().getEntries()) {
        if (entry.getValue().accept(this, param)) {
          return true;
        }
      }
      return false;
    } else {
      return expr.getSubstExpression().accept(this, param);
    }
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expression, P param) {
    return expression.getExpression().accept(this, param) || expression.getTypeOf().accept(this, param);
  }

  @Override
  public Boolean visitInteger(IntegerExpression expr, P params) {
    return false;
  }

  public boolean visitBody(Body body, P param) {
    if (body instanceof Expression) {
      return ((Expression) body).accept(this, param);
    } else if (body instanceof ElimBody) {
      return visitElimBody((ElimBody) body, param);
    } else if (body instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) body;
      for (IntervalElim.CasePair pair : elim.getCases()) {
        if (pair.proj1.accept(this, param) || pair.proj2.accept(this, param)) {
          return true;
        }
      }
      return elim.getOtherwise() != null && visitElimBody(elim.getOtherwise(), param);
    } if (body == null) {
      return false;
    } else {
      throw new IllegalStateException();
    }
  }

  public boolean visitFunction(FunctionDefinition definition, P params) {
    return visitDependentLink(definition.getParameters(), params) || definition.getResultType().accept(this, params) || definition.getResultTypeLevel() != null && definition.getResultTypeLevel().accept(this, params) || visitBody(definition.getBody(), params);
  }

  @Override
  public Boolean visitTypeCoerce(TypeCoerceExpression expr, P params) {
    for (Expression argument : expr.getClauseArguments()) {
      if (argument.accept(this, params)) {
        return true;
      }
    }
    return expr.getArgument().accept(this, params);
  }

  @Override
  public Boolean visitArray(ArrayExpression expr, P params) {
    if (expr.getElementsType().accept(this, params)) {
      return true;
    }
    for (Expression element : expr.getElements()) {
      if (element.accept(this, params)) {
        return true;
      }
    }
    return expr.getTail() != null && expr.getTail().accept(this, params);
  }
}
