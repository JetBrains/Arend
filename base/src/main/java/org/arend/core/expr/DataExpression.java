package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreDataExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataExpression extends Expression implements CoreDataExpression {
  private final Expression myExpression;
  private final Object myMetaData;

  public DataExpression(Expression expression, Object metaData) {
    myExpression = expression;
    myMetaData = metaData;
  }

  @Override
  public @NotNull Expression getExpression() {
    return myExpression;
  }

  @Override
  public @Nullable Object getMetaData() {
    return myMetaData;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitData(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitData(this, param1, param2);
  }

  @Override
  public Decision isWHNF() {
    return Decision.NO;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
