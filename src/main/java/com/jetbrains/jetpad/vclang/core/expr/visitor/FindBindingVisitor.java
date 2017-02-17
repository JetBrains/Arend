package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Referable;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.Map;
import java.util.Set;

public class FindBindingVisitor extends BaseExpressionVisitor<Void, Referable> implements ElimTreeNodeVisitor<Void, Referable> {
  private final Set<? extends Referable> myBindings;

  public FindBindingVisitor(Set<? extends Referable> binding) {
    myBindings = binding;
  }

  @Override
  public Referable visitApp(AppExpression expr, Void params) {
    Referable result = expr.getFunction().accept(this, null);
    if (result != null) {
      return result;
    }
    for (Expression argument : expr.getArguments()) {
      result = argument.accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public Referable visitDefCall(DefCallExpression expr, Void params) {
    for (Expression arg : expr.getDefCallArguments()) {
      Referable result = arg.accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return myBindings.contains(expr.getDefinition()) ? expr.getDefinition() : null;
  }

  @Override
  public Referable visitConCall(ConCallExpression expr, Void params) {
    for (Expression arg : expr.getDataTypeArguments()) {
      Referable result = arg.accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return visitDefCall(expr, null);
  }

  @Override
  public Referable visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : expr.getImplementedHere()) {
      Referable result = entry.getValue().term.accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return visitDefCall(expr, null);
  }

  @Override
  public Referable visitReference(ReferenceExpression expr, Void params) {
    return myBindings.contains(expr.getBinding()) ? expr.getBinding() : null;
  }

  @Override
  public Referable visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : myBindings.contains(expr.getVariable()) ? expr.getVariable() : null;
  }

  @Override
  public Referable visitLam(LamExpression expr, Void params) {
    Referable result = visitDependentLink(expr.getParameters());
    return result != null ? result : expr.getBody().accept(this, null);
  }

  @Override
  public Referable visitPi(PiExpression expr, Void params) {
    Referable result = visitDependentLink(expr.getParameters());
    return result != null ? result : expr.getCodomain().accept(this, null);
  }

  @Override
  public Referable visitUniverse(UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Referable visitError(ErrorExpression expr, Void params) {
    return null;
  }

  @Override
  public Referable visitTuple(TupleExpression expr, Void params) {
    for (Expression field : expr.getFields()) {
      Referable result = field.accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return expr.getType().accept(this, null);
  }

  @Override
  public Referable visitSigma(SigmaExpression expr, Void params) {
    return visitDependentLink(expr.getParameters());
  }

  @Override
  public Referable visitProj(ProjExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  private Referable visitTypeExpression(Type type) {
    if (type.toExpression() != null) {
      return type.toExpression().accept(this, null);
    }
    return visitDependentLink(type.getPiParameters());
  }

  private Referable visitDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      Referable result = visitTypeExpression(link.getType());
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public Referable visitNew(NewExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Referable visitLet(LetExpression letExpression, Void params) {
    for (LetClause clause : letExpression.getClauses()) {
      Referable result = visitLetClause(clause);
      if (result != null) {
        return result;
      }
    }
    return letExpression.getExpression().accept(this, null);
  }

  @Override
  public Referable visitOfType(OfTypeExpression expr, Void params) {
    Referable result = expr.getExpression().accept(this, null);
    return result != null ? result : visitTypeExpression(expr.getType());
  }

  public Referable visitLetClause(LetClause clause) {
    Referable result = visitDependentLink(clause.getParameters());
    if (result != null) {
      return result;
    }
    if (clause.getResultType() != null) {
      result = visitTypeExpression(clause.getResultType());
      if (result != null) {
        return result;
      }
    }
    return clause.getElimTree().accept(this, null);
  }

  @Override
  public Referable visitBranch(BranchElimTreeNode branchNode, Void params) {
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      Referable result = clause.getChild().accept(this, null);
      if (result != null) {
        return result;
      }
    }
    if (branchNode.getOtherwiseClause() != null) {
      Referable result = branchNode.getOtherwiseClause().getChild().accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public Referable visitLeaf(LeafElimTreeNode leafNode, Void params) {
    return leafNode.getExpression().accept(this, null);
  }

  @Override
  public Referable visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return null;
  }
}
