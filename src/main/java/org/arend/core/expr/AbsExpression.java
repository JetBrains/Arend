package org.arend.core.expr;

import org.arend.core.context.binding.Binding;
import org.arend.core.subst.ExprSubstitution;

public class AbsExpression {
  private final Binding myBinding;
  private final Expression myExpression;

  public AbsExpression(Binding binding, Expression expression) {
    myBinding = binding;
    myExpression = expression;
  }

  public Binding getBinding() {
    return myBinding;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public Expression apply(Expression argument) {
    return myBinding == null ? myExpression : myExpression.subst(new ExprSubstitution(myBinding, argument));
  }
}
