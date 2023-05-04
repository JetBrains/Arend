package org.arend.core.expr;

import org.arend.core.definition.Definition;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreQNameExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class QNameExpression extends Expression implements CoreQNameExpression {
  private final Definition myDefinition;

  public QNameExpression(Definition definition) {
    myDefinition = definition;
  }

  @Override
  public @NotNull Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitQName(this, params);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitQName(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitQName(this, param1, param2);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
