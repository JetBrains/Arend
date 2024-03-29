package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.let.HaveClause;
import org.arend.ext.variable.Variable;
import org.arend.core.expr.*;
import org.arend.typechecking.visitor.SearchVisitor;

import java.util.Map;
import java.util.Set;

public class FindMissingBindingVisitor extends SearchVisitor<Void> {
  private final Set<Binding> myBindings;
  private Variable myResult = null;

  public FindMissingBindingVisitor(Set<Binding> binding) {
    myBindings = binding;
  }

  Set<Binding> getBindings() {
    return myBindings;
  }

  public Variable getResult() {
    return myResult;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr, Void params) {
    if (!myBindings.contains(expr.getBinding())) {
      myResult = expr.getBinding();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expression, Void param) {
    InferenceVariable var = expression.getVariable();
    if (var != null) {
      for (Binding bound : var.getBounds()) {
        if (!myBindings.contains(bound)) {
          myResult = bound;
          return true;
        }
      }
      return false;
    }
    return super.visitInferenceReference(expression, param);
  }

  @Override
  public Boolean visitSubst(SubstExpression expr, Void param) {
    if (expr.isInferenceVariable()) {
      for (Binding bound : ((InferenceReferenceExpression) expr.getExpression()).getVariable().getBounds()) {
        if (!myBindings.contains(bound) && !expr.getSubstitution().getKeys().contains(bound)) {
          myResult = bound;
          return true;
        }
      }
      for (Map.Entry<Binding, Expression> entry : expr.getSubstitution().getEntries()) {
        if (entry.getValue().accept(this, null)) {
          return true;
        }
      }
      return false;
    } else {
      return expr.getSubstExpression().accept(this, null);
    }
  }

  @Override
  public Boolean visitLam(LamExpression expr, Void params) {
    if (visitDependentLink(expr.getParameters(), null)) {
      return true;
    }

    boolean found = expr.getBody().accept(this, null);
    freeParameters(expr.getParameters());
    return found;
  }

  @Override
  public Boolean visitPi(PiExpression expr, Void params) {
    if (visitDependentLink(expr.getParameters(), null)) {
      return true;
    }

    boolean found = expr.getCodomain().accept(this, null);
    freeParameters(expr.getParameters());
    return found;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr, Void params) {
    if (visitDependentLink(expr.getParameters(), null)) {
      return true;
    }

    freeParameters(expr.getParameters());
    return false;
  }

  @Override
  protected boolean visitDependentLink(DependentLink parameters, Void params) {
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      if (link1.getTypeExpr().accept(this, null)) {
        for (; parameters != link; parameters = parameters.getNext()) {
          myBindings.remove(parameters);
        }
        return true;
      }

      for (; link != link1; link = link.getNext()) {
        myBindings.add(link);
      }
      myBindings.add(link);
    }
    return false;
  }

  void freeParameters(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      myBindings.remove(link);
    }
  }

  @Override
  public Boolean visitLet(LetExpression letExpression, Void params) {
    for (HaveClause clause : letExpression.getClauses()) {
      if (clause.getExpression().accept(this, null)) {
        return true;
      }
      myBindings.add(clause);
    }

    boolean found = letExpression.getExpression().accept(this, null);
    letExpression.getClauses().forEach(myBindings::remove);
    return found;
  }

  @Override
  public Boolean visitCase(CaseExpression expr, Void params) {
    for (Expression argument : expr.getArguments()) {
      if (argument.accept(this, null)) {
        return true;
      }
    }

    if (visitDependentLink(expr.getParameters(), null)) {
      return true;
    }

    boolean found = expr.getResultType().accept(this, null);
    if (!found && expr.getResultTypeLevel() != null) {
      found = expr.getResultTypeLevel().accept(this, null);
    }
    freeParameters(expr.getParameters());
    return found || visitElimBody(expr.getElimBody(), null);
  }

  @Override
  protected boolean visitElimBody(ElimBody elimBody, Void params) {
    for (var clause : elimBody.getClauses()) {
      if (visitDependentLink(clause.getParameters(), null)) {
        return true;
      }
      if (clause.getExpression() != null) {
        if (clause.getExpression().accept(this, null)) {
          return true;
        }
      }
      freeParameters(clause.getParameters());
    }

    return false;
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expression, Void param) {
    myBindings.add(expression.getThisBinding());
    Boolean result = super.visitClassCall(expression, param);
    myBindings.remove(expression.getThisBinding());
    return result;
  }
}
