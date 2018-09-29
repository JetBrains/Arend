package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;

import java.util.HashSet;
import java.util.Set;

public class FreeVariablesCollector extends VoidExpressionVisitor<Void> {
  private final Set<Binding> myResult = new HashSet<>();

  public Set<Binding> getResult() {
    return myResult;
  }

  public static Set<Binding> getFreeVariables(Expression expression) {
    FreeVariablesCollector collector = new FreeVariablesCollector();
    expression.accept(collector, null);
    return collector.myResult;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    myResult.add(expr.getBinding());
    return null;
  }
}
