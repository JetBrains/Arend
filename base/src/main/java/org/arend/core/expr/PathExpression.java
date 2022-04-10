package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CorePathExpression;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PathExpression extends Expression implements CorePathExpression {
  private LevelPair myLevels;
  private final Expression myArgumentType;
  private final Expression myArgument;

  public PathExpression(LevelPair levels, Expression argumentType, Expression argument) {
    myLevels = levels;
    myArgumentType = argumentType;
    myArgument = argument;
  }

  @Override
  public @NotNull LevelPair getLevels() {
    return myLevels;
  }

  public void setLevels(LevelPair levels) {
    myLevels = levels;
  }

  @Override
  public @NotNull Expression getArgumentType() {
    return myArgumentType;
  }

  @Override
  public @NotNull Expression getArgument() {
    return myArgument;
  }

  public void substLevels(LevelSubstitution substitution) {
    myLevels = myLevels.subst(substitution);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPath(this, params);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPath(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitPath(this, param1, param2);
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
