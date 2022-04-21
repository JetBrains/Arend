package org.arend.core.elimtree;

import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.InferenceReferenceExpression;
import org.arend.ext.core.body.CoreIntervalElim;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IntervalElim implements Body, CoreIntervalElim {
  private final int myNumberOfParameters;
  private final List<CasePair> myCases;
  private final ElimBody myOtherwise;

  public IntervalElim(int numberOfParameters, List<CasePair> cases, ElimBody otherwise) {
    myNumberOfParameters = numberOfParameters;
    myCases = cases;
    myOtherwise = otherwise;
  }

  public static class CasePair extends Pair<Expression, Expression> implements CoreIntervalElim.CasePair {
    public CasePair(Expression proj1, Expression proj2) {
      super(proj1, proj2);
    }

    @Nullable
    @Override
    public CoreExpression getLeftCase() {
      return proj1;
    }

    @Nullable
    @Override
    public CoreExpression getRightCase() {
      return proj2;
    }
  }

  public int getNumberOfParameters() {
    return myNumberOfParameters;
  }

  @NotNull
  @Override
  public List<CasePair> getCases() {
    return myCases;
  }

  @Override
  public ElimBody getOtherwise() {
    return myOtherwise;
  }

  public int getOffset() {
    return myNumberOfParameters - myCases.size();
  }

  public int getNumberOfTotalElim() {
    int result = 0;
    for (Pair<Expression, Expression> pair : myCases) {
      if (pair.proj1 != null && pair.proj2 != null) {
        result++;
      }
    }
    return result;
  }

  @Override
  public Decision isWHNF(List<? extends Expression> arguments) {
    int offset = getOffset();
    Decision result = Decision.YES;
    for (int i = 0; i < myCases.size(); i++) {
      Expression arg = arguments.get(offset + i);
      ConCallExpression conCall = arg.cast(ConCallExpression.class);
      if (conCall != null && (conCall.getDefinition() == Prelude.LEFT && myCases.get(i).proj1 != null || conCall.getDefinition() == Prelude.RIGHT && myCases.get(i).proj2 != null)) {
        return Decision.NO;
      }

      Decision decision = arg.isWHNF();
      if (decision == Decision.NO) {
        return Decision.NO;
      }
      if (decision == Decision.MAYBE) {
        result = Decision.MAYBE;
      }
    }
    return myOtherwise == null ? result : result.min(myOtherwise.isWHNF(arguments));
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    int offset = getOffset();
    for (int i = 0; i < myCases.size(); i++) {
      ConCallExpression conCall = arguments.get(offset + i).cast(ConCallExpression.class);
      if (conCall != null && (conCall.getDefinition() == Prelude.LEFT && myCases.get(i).proj1 != null || conCall.getDefinition() == Prelude.RIGHT && myCases.get(i).proj2 != null)) {
        return null;
      }
    }

    Expression stuck = myOtherwise == null ? null : myOtherwise.getStuckExpression(arguments, expression);
    if (stuck != null && stuck.isInstance(ErrorExpression.class)) {
      return stuck;
    }

    InferenceReferenceExpression refStuck = stuck != null ? stuck.cast(InferenceReferenceExpression.class) : null;
    while (refStuck != null && refStuck.getSubstExpression() != null) {
      refStuck = refStuck.getSubstExpression().cast(InferenceReferenceExpression.class);
    }
    for (int i = 0; i < myCases.size(); i++) {
      stuck = arguments.get(offset + i).getStuckExpression();
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
    }

    return refStuck;
  }
}
