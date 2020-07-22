package org.arend.core.expr.visitor;

import org.arend.ext.variable.Variable;
import org.arend.core.expr.*;
import org.arend.typechecking.visitor.SearchVisitor;

import java.util.Set;

public class FindBindingVisitor extends SearchVisitor<Void> {
  private final Set<? extends Variable> myBindings;
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
  protected boolean processDefCall(DefCallExpression expression, Void param) {
    if (myBindings.contains(expression.getDefinition())) {
      myResult = expression.getDefinition();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr, Void params) {
    if (myBindings.contains(expr.getBinding())) {
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
}
