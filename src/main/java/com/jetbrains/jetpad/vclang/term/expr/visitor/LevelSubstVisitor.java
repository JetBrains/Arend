package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class LevelSubstVisitor extends BaseExpressionVisitor<Void, Void> implements ElimTreeNodeVisitor<Void, Void> {
  private LevelSubstitution myLevelSubst;

  public LevelSubstVisitor(LevelSubstitution subst) {
    myLevelSubst = subst;
  }

  public static Expression subst(Expression expr, LevelSubstitution subst) {
    SubstVisitor substVisitor = new SubstVisitor(new ExprSubstitution());
    expr = expr.accept(substVisitor, null);
    expr.accept(new LevelSubstVisitor(subst), null);
    return expr;
  }

  public static ElimTreeNode subst(ElimTreeNode node, LevelSubstitution subst) {
    SubstVisitor substVisitor = new SubstVisitor(new ExprSubstitution());
    node = node.accept(substVisitor, null);
    node.accept(new LevelSubstVisitor(subst), null);
    return node;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    expr.applyLevelSubst(myLevelSubst);
    return null;
  }

  /*@Override
  public Void visitConCall(ConCallExpression expr, Void params) {
    if (expr.getDataTypeArguments().isEmpty()) return null;
    for (Expression parameter : expr.getDataTypeArguments()) {
      parameter.accept(this, null);
    }
    return null;
  } /**/

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitApp(AppExpression expr, Void params) {
    expr.getFunction().accept(this, params);
    for (Expression argument : expr.getArguments()) {
      argument.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    if (expr.getBinding() instanceof InferenceBinding) {
      //((InferenceBinding) expr.getBinding()).setType(subst(expr.getBinding().getType(), myLevelSubst));
      expr.getBinding().getType().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr, Void params) {
    expr.getBody().accept(this, params);
    for (DependentLink param = expr.getParameters(); param.hasNext(); param = param.getNext()) {
      param.getType().accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, Void params) {
    for (DependentLink param = expr.getParameters(); param.hasNext(); param = param.getNext()) {
      param.getType().accept(this, params);
    }
    expr.getCodomain().accept(this, params);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Void params) {
    for (DependentLink param = expr.getParameters(); param.hasNext(); param = param.getNext()) {
      param.getType().accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Void params) {
    expr.setUniverse(expr.getUniverse().subst(myLevelSubst));
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr, Void params) {
    for (Expression field : expr.getFields()) {
      field.accept(this, null);
    }
    expr.getType().accept(this, null);
    return null;
  }

  @Override
  public Void visitProj(ProjExpression expr, Void params) {
    expr.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, Void params) {
    expr.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitLet(LetExpression expr, Void params) {
    for (LetClause clause : expr.getClauses()) {
      for (DependentLink param = clause.getParameters(); param.hasNext(); param = param.getNext()) {
        param.getType().accept(this, null);
      }
      clause.getElimTree().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitOfType(OfTypeExpression expr, Void params) {
    expr.getExpression().accept(this, params);
    expr.getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitBranch(BranchElimTreeNode branchNode, Void params) {
    for (Binding binding : branchNode.getContextTail()) {
      visitReference(Reference(binding), null);
    }

    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      clause.getChild().accept(this, null);
    }

    if (branchNode.getOtherwiseClause() != null) {
      branchNode.getOtherwiseClause().getChild().accept(this, null);
    }

    return null;
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode leafNode, Void params) {
    leafNode.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return null;
  }
  /**/
}
