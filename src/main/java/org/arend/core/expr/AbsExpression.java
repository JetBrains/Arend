package org.arend.core.expr;

import org.arend.core.context.binding.TypedBinding;
import org.arend.core.subst.ExprSubstitution;

public class AbsExpression {
  private final TypedBinding myBinding;
  private final Expression myExpression;

  public AbsExpression(TypedBinding binding, Expression expression) {
    myBinding = binding;
    myExpression = expression;
  }

  public TypedBinding getBinding() {
    return myBinding;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public Expression apply(Expression argument) {
    return myBinding == null ? myExpression : myExpression.subst(new ExprSubstitution(myBinding, argument));
  }

  public boolean isBindingUsed() {
    return myBinding != null && myExpression.findBinding(myBinding);
  }
}
