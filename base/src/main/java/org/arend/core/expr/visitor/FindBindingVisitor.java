package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.let.HaveClause;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.variable.Variable;
import org.arend.core.expr.*;
import org.arend.typechecking.visitor.SearchVisitor;

import java.util.HashSet;
import java.util.Set;

public class FindBindingVisitor extends SearchVisitor<Void> {
  private final Set<? extends Variable> myBindings;
  private final Set<Binding> myAllowedBindings = new HashSet<>();
  private Variable myResult = null;

  public FindBindingVisitor(Set<? extends Variable> binding) {
    myBindings = binding;
  }

  Set<? extends Variable> getBindings() {
    return myBindings;
  }

  public Variable getResult() {
    return myResult;
  }

  @Override
  protected CoreExpression.FindAction processDefCall(DefCallExpression expression, Void param) {
    if (myBindings.contains(expression.getDefinition())) {
      myResult = expression.getDefinition();
      return CoreExpression.FindAction.STOP;
    } else {
      return CoreExpression.FindAction.CONTINUE;
    }
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr, Void params) {
    if (myBindings.contains(expr.getBinding()) && !myAllowedBindings.contains(expr.getBinding())) {
      myResult = expr.getBinding();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() != null) {
      return expr.getSubstExpression().accept(this, null);
    }
    if (myBindings.contains(expr.getVariable())) {
      myResult = expr.getVariable();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr, Void param) {
    myAllowedBindings.add(expr.getThisBinding());
    Boolean result = super.visitClassCall(expr, param);
    myAllowedBindings.remove(expr.getThisBinding());
    return result;
  }

  @Override
  protected boolean visitDependentLink(DependentLink link, Void param) {
    for (; link.hasNext(); link = link.getNext()) {
      if (link instanceof TypedDependentLink && link.getTypeExpr().accept(this, param)) {
        return true;
      }
      myAllowedBindings.add(link);
    }
    return false;
  }

  private void freeDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      myAllowedBindings.remove(link);
    }
  }

  @Override
  public Boolean visitLam(LamExpression expr, Void param) {
    if (visitDependentLink(expr.getParameters(), null)) return true;
    Boolean result = expr.getBody().accept(this, param);
    freeDependentLink(expr.getParameters());
    return result;
  }

  @Override
  public Boolean visitPi(PiExpression expr, Void param) {
    if (visitDependentLink(expr.getParameters(), null)) return true;
    Boolean result = expr.getCodomain().accept(this, param);
    freeDependentLink(expr.getParameters());
    return result;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr, Void param) {
    if (visitDependentLink(expr.getParameters(), null)) return true;
    freeDependentLink(expr.getParameters());
    return false;
  }

  @Override
  public Boolean visitLet(LetExpression expression, Void param) {
    for (HaveClause lc : expression.getClauses()) {
      if (lc.getExpression().accept(this, param)) {
        return true;
      }
      myAllowedBindings.add(lc);
    }

    Boolean result = expression.getExpression().accept(this, param);
    for (HaveClause lc : expression.getClauses()) {
      myAllowedBindings.remove(lc);
    }
    return result;
  }

  @Override
  protected boolean visitElimBody(ElimBody elimBody, Void param) {
    for (var clause : elimBody.getClauses()) {
      if (visitDependentLink(clause.getParameters(), null) || clause.getExpression() != null && clause.getExpression().accept(this, param)) {
        return true;
      }
      freeDependentLink(clause.getParameters());
    }
    return false;
  }

  @Override
  public Boolean visitCase(CaseExpression expr, Void param) {
    if (expr.getArguments().stream().anyMatch(arg -> arg.accept(this, param)) || visitDependentLink(expr.getParameters(), param)) return true;
    if (expr.getResultType().accept(this, param) || expr.getResultTypeLevel() != null && expr.getResultTypeLevel().accept(this, param)) return true;
    freeDependentLink(expr.getParameters());
    return visitElimBody(expr.getElimBody(), param);
  }
}
