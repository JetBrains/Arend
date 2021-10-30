package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.jetbrains.annotations.NotNull;

public class PersistentEvaluatingBinding extends NamedBinding implements EvaluatingBinding {
  private Expression myExpression;

  public PersistentEvaluatingBinding(String name, Expression expression) {
    super(name);
    myExpression = expression;
  }

  @Override
  public Expression getTypeExpr() {
    return myExpression.getType();
  }

  @Override
  public void strip(StripVisitor stripVisitor) {
    myExpression = myExpression.accept(stripVisitor, null);
  }

  @Override
  public void subst(InPlaceLevelSubstVisitor visitor) {
    myExpression.accept(visitor, null);
  }

  @Override
  public @NotNull Expression getExpression() {
    return myExpression;
  }

  public void setExpression(Expression expr) {
    myExpression = expr;
  }
}
