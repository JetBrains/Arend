package org.arend.core.expr;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.expr.CoreAtExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class AtExpression extends Expression implements CoreAtExpression {
  private LevelPair myLevels;
  private final Expression myPathArgument;
  private final Expression myIntervalArgument;

  private AtExpression(LevelPair levels, Expression pathArgument, Expression intervalArgument) {
    myLevels = levels;
    myPathArgument = pathArgument;
    myIntervalArgument = intervalArgument;
  }

  public static Expression make(LevelPair levels, Expression pathArgument, Expression intervalArgument, boolean checkInterval) {
    if (pathArgument instanceof PathExpression) {
      return AppExpression.make(((PathExpression) pathArgument).getArgument(), intervalArgument, true);
    }
    if (checkInterval && intervalArgument instanceof ConCallExpression) {
      Constructor constructor = ((ConCallExpression) intervalArgument).getDefinition();
      if (constructor == Prelude.LEFT || constructor == Prelude.RIGHT) {
        Expression type = pathArgument.getType().normalize(NormalizationMode.WHNF);
        if (type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.PATH) {
          return constructor == Prelude.LEFT ? ((DataCallExpression) type).getDefCallArguments().get(1) : ((DataCallExpression) type).getDefCallArguments().get(2);
        }
      }
    }
    return new AtExpression(levels, pathArgument, intervalArgument);
  }

  @Override
  public @NotNull LevelPair getLevels() {
    return myLevels;
  }

  @Override
  public @NotNull Expression getPathArgument() {
    return myPathArgument;
  }

  @Override
  public @NotNull Expression getIntervalArgument() {
    return myIntervalArgument;
  }

  public void substLevels(LevelSubstitution substitution) {
    myLevels = myLevels.subst(substitution);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitAt(this, params);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitAt(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitAt(this, param1, param2);
  }

  @Override
  public Decision isWHNF() {
    ConCallExpression conCall = myIntervalArgument.cast(ConCallExpression.class);
    if (conCall != null && (conCall.getDefinition() == Prelude.LEFT || conCall.getDefinition() == Prelude.RIGHT) || myPathArgument.isInstance(PathExpression.class)) {
      return Decision.NO;
    }
    Decision decision = myIntervalArgument.isWHNF();
    if (decision == Decision.NO) {
      return Decision.NO;
    }
    Decision decision1 = myPathArgument.isWHNF();
    if (decision1 == Decision.NO) {
      return Decision.NO;
    }
    return decision.min(decision1);
  }

  @Override
  public Expression getStuckExpression() {
    ConCallExpression conCall = myIntervalArgument.cast(ConCallExpression.class);
    if (conCall != null && (conCall.getDefinition() == Prelude.LEFT || conCall.getDefinition() == Prelude.RIGHT) || myPathArgument.isInstance(PathExpression.class)) {
      return null;
    }

    Expression stuck = myPathArgument.getStuckExpression();
    if (stuck != null && stuck.isInstance(ErrorExpression.class)) {
      return stuck;
    }

    InferenceReferenceExpression refStuck = stuck != null ? stuck.cast(InferenceReferenceExpression.class) : null;
    while (refStuck != null && refStuck.getSubstExpression() != null) {
      refStuck = refStuck.getSubstExpression().cast(InferenceReferenceExpression.class);
    }

    stuck = myIntervalArgument.getStuckExpression();
    if (stuck != null) {
      if (stuck.isInstance(ErrorExpression.class)) {
        return stuck;
      }
      if (refStuck == null) {
        refStuck = stuck.cast(InferenceReferenceExpression.class);
        while (refStuck != null && refStuck.getSubstExpression() != null) {
          refStuck = refStuck.getSubstExpression().cast(InferenceReferenceExpression.class);
        }
      }
    }

    return refStuck;
  }
}
