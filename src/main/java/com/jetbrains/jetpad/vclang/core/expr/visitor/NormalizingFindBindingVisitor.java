package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;

import java.util.Collections;
import java.util.Map;

public class NormalizingFindBindingVisitor extends BaseExpressionVisitor<Void, Boolean> {
  private final FindBindingVisitor myVisitor;

  private NormalizingFindBindingVisitor(Variable binding) {
    myVisitor = new FindBindingVisitor(Collections.singleton(binding));
  }

  public static boolean findBinding(Expression expression, Variable binding) {
    return new NormalizingFindBindingVisitor(binding).findBinding(expression, true);
  }

  private boolean findBinding(Expression expression, boolean normalize) {
    if (expression.accept(myVisitor, null) == null) {
      return false;
    }
    return (normalize ? expression.normalize(NormalizeVisitor.Mode.WHNF) : expression).accept(this, null);
  }

  @Override
  public Boolean visitApp(AppExpression expr, Void params) {
    return findBinding(expr.getFunction(), false) || findBinding(expr.getArgument(), true);
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr, Void params) {
    for (Expression arg : expr.getDefCallArguments()) {
      if (findBinding(arg, true)) {
        return true;
      }
    }
    return myVisitor.getBindings().contains(expr.getDefinition());
  }

  @Override
  public Boolean visitConCall(ConCallExpression expr, Void params) {
    for (Expression arg : expr.getDataTypeArguments()) {
      if (findBinding(arg, true)) {
        return true;
      }
    }
    return visitDefCall(expr, null);
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : expr.getImplementedHere()) {
      if (findBinding(entry.getValue().term, true)) {
        return true;
      }
    }
    return visitDefCall(expr, null);
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr, Void params) {
    return myVisitor.getBindings().contains(expr.getBinding());
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() != null) {
      return findBinding(expr.getSubstExpression(), true);
    } else {
      return myVisitor.getBindings().contains(expr.getVariable());
    }
  }

  @Override
  public Boolean visitLam(LamExpression expr, Void params) {
    return visitDependentLink(expr.getParameters()) || findBinding(expr.getBody(), true);
  }

  @Override
  public Boolean visitPi(PiExpression expr, Void params) {
    return visitDependentLink(expr.getParameters()) || findBinding(expr.getCodomain(), true);
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
      if (findBinding(field, true)) {
        return true;
      }
    }

    return findBinding(expr.getSigmaType(), false);
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr, Void params) {
    return visitDependentLink(expr.getParameters());
  }

  @Override
  public Boolean visitProj(ProjExpression expr, Void params) {
    return findBinding(expr.getExpression(), false);
  }

  private boolean visitDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      if (findBinding(link.getTypeExpr(), true)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitNew(NewExpression expr, Void params) {
    return visitClassCall(expr.getExpression(), null);
  }

  @Override
  public Boolean visitLet(LetExpression letExpression, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public Boolean visitCase(CaseExpression expr, Void params) {
    for (Expression argument : expr.getArguments()) {
      if (findBinding(argument, true)) {
        return true;
      }
    }

    return findBinding(expr.getResultType(), true) || visitDependentLink(expr.getParameters()) || findBindingInElimTree(expr.getElimTree());
  }

  private boolean findBindingInElimTree(ElimTree elimTree) {
    if (visitDependentLink(elimTree.getParameters())) {
      return true;
    }

    if (elimTree instanceof LeafElimTree) {
      return findBinding(((LeafElimTree) elimTree).getExpression(), true);
    } else {
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        if (findBindingInElimTree(entry.getValue())) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expr, Void params) {
    return findBinding(expr.getExpression(), true) || findBinding(expr.getTypeOf(), true);
  }
}
