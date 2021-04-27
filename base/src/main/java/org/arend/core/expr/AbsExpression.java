package org.arend.core.expr;

import org.arend.core.context.binding.Binding;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.expr.CoreAbsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbsExpression implements CoreAbsExpression {
  private final Binding myBinding;
  private final Expression myExpression;

  public AbsExpression(Binding binding, Expression expression) {
    myBinding = binding;
    myExpression = expression;
  }

  @Nullable
  @Override
  public Binding getBinding() {
    return myBinding;
  }

  @NotNull
  @Override
  public Expression getExpression() {
    return myExpression;
  }

  public Expression apply(Expression argument, LevelPair levels) {
    return myBinding == null ? myExpression : myExpression.subst(new ExprSubstitution(myBinding, argument), levels);
  }
}
