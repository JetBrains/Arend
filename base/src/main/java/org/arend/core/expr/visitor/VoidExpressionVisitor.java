package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.util.Pair;

import java.util.Map;

public class VoidExpressionVisitor<P> extends BaseExpressionVisitor<P,Void> {
  @Override
  public Void visitApp(AppExpression expr, P params) {
    expr.getFunction().accept(this, params);
    expr.getArgument().accept(this, params);
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, P params) {
    for (Expression arg : expr.getDefCallArguments()) {
      arg.accept(this, params);
    }
    return null;
  }

  protected void processConCall(ConCallExpression expr, P params) {}

  @Override
  public Void visitConCall(ConCallExpression expr, P params) {
    Expression it = expr;
    do {
      expr = (ConCallExpression) it;
      processConCall(expr, params);

      for (Expression arg : expr.getDataTypeArguments()) {
        arg.accept(this, params);
      }

      int recursiveParam = expr.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        for (Expression arg : expr.getDefCallArguments()) {
          arg.accept(this, params);
        }
        return null;
      }

      for (int i = 0; i < expr.getDefCallArguments().size(); i++) {
        if (i != recursiveParam) {
          expr.getDefCallArguments().get(i).accept(this, params);
        }
      }

      it = expr.getDefCallArguments().get(recursiveParam);
    } while (it instanceof ConCallExpression);

    it.accept(this, params);
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, P params) {
    visitDefCall(expr, params);
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      entry.getValue().accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, P params) {
    return null;
  }

  @Override
  public Void visitInferenceReference(InferenceReferenceExpression expr, P params) {
    if (expr.getSubstExpression() != null) {
      expr.getSubstExpression().accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitSubst(SubstExpression expr, P params) {
    expr.getExpression().accept(this, params);
    for (Map.Entry<Binding, Expression> entry : expr.getSubstitution().getEntries()) {
      entry.getValue().accept(this, params);
    }
    return null;
  }

  public void visitParameters(DependentLink link, P params) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      link.getTypeExpr().accept(this, params);
    }
  }

  @Override
  public Void visitLam(LamExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    expr.getBody().accept(this, params);
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    expr.getCodomain().accept(this, params);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, P params) {
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr, P params) {
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr, P params) {
    visitSigma(expr.getSigmaType(), params);
    for (Expression field : expr.getFields()) {
      field.accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitProj(ProjExpression expr, P params) {
    expr.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, P params) {
    visitClassCall(expr.getClassCall(), params);
    if (expr.getRenewExpression() != null) {
      expr.getRenewExpression().accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitPEval(PEvalExpression expr, P params) {
    expr.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitLet(LetExpression expr, P params) {
    for (HaveClause clause : expr.getClauses()) {
      clause.getExpression().accept(this, params);
    }
    expr.getExpression().accept(this, params);
    return null;
  }

  protected void visitElimTree(ElimTree elimTree, P params) {
  }

  protected void visitElimBody(ElimBody elimBody, P params) {
    for (var clause : elimBody.getClauses()) {
      visitParameters(clause.getParameters(), params);
      if (clause.getExpression() != null) {
        clause.getExpression().accept(this, params);
      }
    }
    visitElimTree(elimBody.getElimTree(), params);
  }

  public void visitBody(Body body, P params) {
    if (body instanceof IntervalElim) {
      for (Pair<Expression, Expression> pair : ((IntervalElim) body).getCases()) {
        if (pair.proj1 != null) {
          pair.proj1.accept(this, params);
        }
        if (pair.proj2 != null) {
          pair.proj2.accept(this, params);
        }
      }
      body = ((IntervalElim) body).getOtherwise();
    }

    if (body instanceof Expression) {
      ((Expression) body).accept(this, params);
    } else if (body instanceof ElimBody) {
      visitElimBody((ElimBody) body, params);
    } else {
      assert body == null;
    }
  }

  @Override
  public Void visitCase(CaseExpression expr, P params) {
    for (Expression arg : expr.getArguments()) {
      arg.accept(this, params);
    }
    visitParameters(expr.getParameters(), params);
    visitElimBody(expr.getElimBody(), params);
    expr.getResultType().accept(this, params);
    if (expr.getResultTypeLevel() != null) {
      expr.getResultTypeLevel().accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitOfType(OfTypeExpression expr, P params) {
    expr.getExpression().accept(this, params);
    expr.getTypeOf().accept(this, params);
    return null;
  }

  @Override
  public Void visitInteger(IntegerExpression expr, P params) {
    return null;
  }

  @Override
  public Void visitTypeCoerce(TypeCoerceExpression expr, P params) {
    for (Expression argument : expr.getClauseArguments()) {
      argument.accept(this, params);
    }
    expr.getArgument().accept(this, params);
    return null;
  }

  @Override
  public Void visitArray(ArrayExpression expr, P params) {
    expr.getElementsType().accept(this, params);
    for (Expression element : expr.getElements()) {
      element.accept(this, params);
    }
    if (expr.getTail() != null) {
      expr.getTail().accept(this, params);
    }
    return null;
  }
}
