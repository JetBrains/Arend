package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.ext.core.elimtree.CoreBranchKey;
import org.arend.ext.core.ops.NormalizationMode;

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
    return (normalize ? expression.normalize(NormalizationMode.WHNF) : expression).accept(this, null);
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
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      if (findBinding(entry.getValue(), true)) {
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
  public Boolean visitSubst(SubstExpression expr, Void params) {
    return expr.getSubstExpression().accept(this, null);
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
    if (visitClassCall(expr.getClassCall(), null)) {
      return true;
    }
    return expr.getRenewExpression() != null && findBinding(expr.getRenewExpression(), true);
  }

  @Override
  public Boolean visitPEval(PEvalExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
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

    return findBinding(expr.getResultType(), true) || expr.getResultTypeLevel() != null && findBinding(expr.getResultTypeLevel(), true) || visitDependentLink(expr.getParameters()) || findBindingInElimTree(expr.getElimTree());
  }

  private boolean findBindingInElimTree(ElimTree elimTree) {
    if (visitDependentLink(elimTree.getParameters())) {
      return true;
    }

    if (elimTree instanceof LeafElimTree) {
      return findBinding(((LeafElimTree) elimTree).getExpression(), true);
    } else {
      for (Map.Entry<CoreBranchKey, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
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

  @Override
  public Boolean visitInteger(IntegerExpression expr, Void params) {
    return false;
  }
}
