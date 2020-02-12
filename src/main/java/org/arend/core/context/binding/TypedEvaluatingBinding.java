package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;

import javax.annotation.Nonnull;

public class TypedEvaluatingBinding extends TypedBinding implements EvaluatingBinding {
  private Expression myExpression;

  public TypedEvaluatingBinding(String name, Expression expression, Expression type) {
    super(name, type);
    myExpression = expression;
  }

  @Nonnull
  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public void strip(StripVisitor stripVisitor) {
    super.strip(stripVisitor);
    myExpression = myExpression.accept(stripVisitor, null);
  }

  @Override
  public Expression subst(SubstVisitor visitor) {
    return myExpression.accept(visitor, null);
  }

  @Override
  public void subst(InPlaceLevelSubstVisitor visitor) {
    myExpression.accept(visitor, null);
  }
}
