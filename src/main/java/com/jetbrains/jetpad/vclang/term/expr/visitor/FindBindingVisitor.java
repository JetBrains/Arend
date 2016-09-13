package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.Map;
import java.util.Set;

public class FindBindingVisitor extends BaseExpressionVisitor<Void, Boolean> implements ElimTreeNodeVisitor<Void, Boolean> {
  private final Set<? extends Referable> myBindings;

  public FindBindingVisitor(Set<? extends Referable> binding) {
    myBindings = binding;
  }

  @Override
  public Boolean visitApp(AppExpression expr, Void params) {
    if (expr.getFunction().accept(this, null)) {
      return true;
    }
    for (Expression argument : expr.getArguments()) {
      if (argument.accept(this, null)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr, Void params) {
    return myBindings.contains(expr.getDefinition());
  }

  @Override
  public Boolean visitFieldCall(FieldCallExpression expr, Void params) {
    return myBindings.contains(expr.getDefinition()) || expr.getExpression().accept(this, null);
  }

  @Override
  public Boolean visitConCall(ConCallExpression expr, Void params) {
    if (myBindings.contains(expr.getDefinition())) {
      return true;
    }
    for (Expression parameter : expr.getDataTypeArguments()) {
      if (parameter.accept(this, null)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr, Void params) {
    if (myBindings.contains(expr.getDefinition())) {
      return true;
    }
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : expr.getImplementedHere()) {
      if (entry.getValue().term.accept(this, null)) return true;
    }
    return false;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr, Void params) {
    return myBindings.contains(expr.getBinding());
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : myBindings.contains(expr.getVariable());
  }

  @Override
  public Boolean visitLam(LamExpression expr, Void params) {
    return visitDependentLink(expr.getParameters()) || expr.getBody().accept(this, null);
  }

  @Override
  public Boolean visitPi(PiExpression expr, Void params) {
    return visitDependentLink(expr.getParameters()) || expr.getCodomain().accept(this, null);
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr, Void params) {
    return false;
  }

  @Override
  public Boolean visitError(ErrorExpression expr, Void params) {
    return false;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr, Void params) {
    for (Expression field : expr.getFields()) {
      if (field.accept(this, null)) return true;
    }
    return false;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr, Void params) {
    return visitDependentLink(expr.getParameters());
  }

  @Override
  public Boolean visitProj(ProjExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  private boolean visitDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      if (link.getType().accept(this, null)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitNew(NewExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Boolean visitLet(LetExpression letExpression, Void params) {
    for (LetClause clause : letExpression.getClauses()) {
      if (visitLetClause(clause)) return true;
    }
    return letExpression.getExpression().accept(this, null);
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null) || expr.getType().accept(this, null);
  }

  public boolean visitLetClause(LetClause clause) {
    if (visitDependentLink(clause.getParameters())) return true;
    if (clause.getResultType() != null && clause.getResultType().accept(this, null)) return true;
    return clause.getElimTree().accept(this, null);
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, Void params) {
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      if (clause.getChild().accept(this, null))
        return true;
    }
    if (branchNode.getOtherwiseClause() != null) {
      if (branchNode.getOtherwiseClause().getChild().accept(this, null))
        return true;
    }
    return false;
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, Void params) {
    return leafNode.getExpression().accept(this, null);
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return false;
  }
}
