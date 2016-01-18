package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.Map;
import java.util.Set;

public class FindBindingVisitor extends BaseExpressionVisitor<Void, Boolean> implements ElimTreeNodeVisitor<Void, Boolean> {
  private final Set<Binding> myBindings;

  public FindBindingVisitor(Set<Binding> binding) {
    myBindings = binding;
  }

  @Override
  public Boolean visitApp(AppExpression expr, Void params) {
    return expr.getFunction().accept(this, null) || expr.getArgument().getExpression().accept(this, null);
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr, Void params) {
    return myBindings.contains(expr.getDefinition());
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
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      if (elem.getValue().type != null && elem.getValue().type.accept(this, null) || elem.getValue().term != null && elem.getValue().term.accept(this, null)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr, Void params) {
    return myBindings.contains(expr.getBinding());
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
  public Boolean visitInferHole(InferHoleExpression expr, Void params) {
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
    for (; link != null; link = link.getNext()) {
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
