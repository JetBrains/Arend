package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.BaseExpressionVisitor;

public abstract class ProcessDefCallsVisitor<P> extends BaseExpressionVisitor<P, Boolean> {
  protected abstract boolean processDefCall(DefCallExpression expression, P param);

  @Override
  public Boolean visitDefCall(DefCallExpression expression, P param) {
    return processDefCall(expression, param) || expression.getDefCallArguments().stream().anyMatch(arg -> arg.accept(this, param));
  }

  @Override
  public Boolean visitConCall(ConCallExpression expression, P param) {
    return visitDefCall(expression, param) || expression.getDataTypeArguments().stream().anyMatch(arg -> arg.accept(this, param));
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

  @Override
  public Boolean visitCase(CaseExpression expr, P param) {
    return visitElimTree(expr.getElimTree(), param) || visitDependentLink(expr.getParameters(), param) || expr.getResultType().accept(this, param) || expr.getArguments().stream().anyMatch(arg -> arg.accept(this, param));
  }

  private boolean visitElimTree(ElimTree elimTree, P param) {
    if (visitDependentLink(elimTree.getParameters(), param)) {
      return true;
    }
    if (elimTree instanceof LeafElimTree) {
      return ((LeafElimTree) elimTree).getExpression().accept(this, param);
    } else {
      return ((BranchElimTree) elimTree).getChildren().stream().anyMatch(entry -> visitElimTree(entry.getValue(), param));
    }
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expression, P param) {
    return visitDefCall(expression, param) || expression.getFieldSet().getImplemented().stream().anyMatch(entry -> entry.getValue().term.accept(this, param));
  }

  @Override
  public Boolean visitLetClauseCall(LetClauseCallExpression expr, P param) {
    return false;
  }

  private boolean visitDependentLink(DependentLink link, P param) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      if (link.getType().getExpr().accept(this, param)) {
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
    return expression.getExpression().accept(this, param);
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
  public Boolean visitOfType(OfTypeExpression expression, P param) {
    return expression.getExpression().accept(this, param) || expression.getTypeOf().accept(this, param);
  }
}
