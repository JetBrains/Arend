package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;

import java.util.Map;
import java.util.Set;

public class FindBindingVisitor extends BaseExpressionVisitor<Void, Variable> {
  private final Set<? extends Variable> myBindings;

  public FindBindingVisitor(Set<? extends Variable> binding) {
    myBindings = binding;
  }

  Set<? extends Variable> getBindings() {
    return myBindings;
  }

  @Override
  public Variable visitApp(AppExpression expr, Void params) {
    Variable result = expr.getFunction().accept(this, null);
    if (result != null) {
      return result;
    }
    return expr.getArgument().accept(this, null);
  }

  @Override
  public Variable visitDefCall(DefCallExpression expr, Void params) {
    for (Expression arg : expr.getDefCallArguments()) {
      Variable result = arg.accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return myBindings.contains(expr.getDefinition()) ? expr.getDefinition() : null;
  }

  @Override
  public Variable visitConCall(ConCallExpression expr, Void params) {
    for (Expression arg : expr.getDataTypeArguments()) {
      Variable result = arg.accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return visitDefCall(expr, null);
  }

  @Override
  public Variable visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, AbsExpression> entry : expr.getImplementedHere().entrySet()) {
      Variable result = entry.getValue().getExpression().accept(this, null);
      if (result != null) {
        return result;
      }
      result = entry.getValue().getBinding().getTypeExpr().accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return visitDefCall(expr, null);
  }

  @Override
  public Variable visitReference(ReferenceExpression expr, Void params) {
    return myBindings.contains(expr.getBinding()) ? expr.getBinding() : null;
  }

  @Override
  public Variable visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : myBindings.contains(expr.getVariable()) ? expr.getVariable() : null;
  }

  @Override
  public Variable visitSubst(SubstExpression expr, Void params) {
    return expr.getSubstExpression().accept(this, null);
  }

  @Override
  public Variable visitLam(LamExpression expr, Void params) {
    Variable result = visitDependentLink(expr.getParameters());
    return result != null ? result : expr.getBody().accept(this, null);
  }

  @Override
  public Variable visitPi(PiExpression expr, Void params) {
    Variable result = visitDependentLink(expr.getParameters());
    return result != null ? result : expr.getCodomain().accept(this, null);
  }

  @Override
  public Variable visitUniverse(UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Variable visitError(ErrorExpression expr, Void params) {
    return null;
  }

  @Override
  public Variable visitTuple(TupleExpression expr, Void params) {
    for (Expression field : expr.getFields()) {
      Variable result = field.accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return expr.getSigmaType().accept(this, null);
  }

  @Override
  public Variable visitSigma(SigmaExpression expr, Void params) {
    return visitDependentLink(expr.getParameters());
  }

  @Override
  public Variable visitProj(ProjExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  private Variable visitDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      Variable result = link.getTypeExpr().accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public Variable visitNew(NewExpression expr, Void params) {
    Variable result = visitClassCall(expr.getClassCall(), null);
    return result != null ? result : expr.getRenewExpression() == null ? null : expr.getRenewExpression().accept(this, null);
  }

  @Override
  public Variable visitPEval(PEvalExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Variable visitLet(LetExpression letExpression, Void params) {
    for (LetClause clause : letExpression.getClauses()) {
      Variable result = clause.getExpression().accept(this, null);
      if (result != null) {
        return result;
      }
    }
    return letExpression.getExpression().accept(this, null);
  }

  @Override
  public Variable visitCase(CaseExpression expr, Void params) {
    for (Expression argument : expr.getArguments()) {
      Variable result = argument.accept(this, null);
      if (result != null) {
        return result;
      }
    }

    Variable result = expr.getResultType().accept(this, null);
    if (result != null) {
      return result;
    }

    if (expr.getResultTypeLevel() != null) {
      result = expr.getResultTypeLevel().accept(this, null);
      if (result != null) {
        return result;
      }
    }

    result = visitDependentLink(expr.getParameters());
    if (result != null) {
      return result;
    }

    return findBindingInElimTree(expr.getElimTree());
  }

  private Variable findBindingInElimTree(ElimTree elimTree) {
    Variable result = visitDependentLink(elimTree.getParameters());
    if (result != null) {
      return result;
    }

    if (elimTree instanceof LeafElimTree) {
      result = ((LeafElimTree) elimTree).getExpression().accept(this, null);
    } else {
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        result = findBindingInElimTree(entry.getValue());
        if (result != null) {
          return result;
        }
      }
    }

    return result;
  }

  @Override
  public Variable visitOfType(OfTypeExpression expr, Void params) {
    Variable result = expr.getExpression().accept(this, null);
    return result != null ? result : expr.getTypeOf().accept(this, null);
  }

  @Override
  public Variable visitInteger(IntegerExpression expr, Void params) {
    return null;
  }
}
