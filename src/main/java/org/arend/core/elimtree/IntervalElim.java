package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.InferenceReferenceExpression;
import org.arend.prelude.Prelude;
import org.arend.util.Pair;

import java.util.List;

public class IntervalElim implements Body {
  private final DependentLink myParameters;
  private final List<Pair<Expression, Expression>> myCases;
  private final ElimTree myOtherwise;

  public IntervalElim(DependentLink parameters, List<Pair<Expression, Expression>> cases, ElimTree otherwise) {
    myParameters = parameters;
    myCases = cases;
    myOtherwise = otherwise;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public List<Pair<Expression, Expression>> getCases() {
    return myCases;
  }

  public ElimTree getOtherwise() {
    return myOtherwise;
  }

  @Override
  public boolean isWHNF(List<? extends Expression> arguments) {
    int offset = DependentLink.Helper.size(myParameters) - myCases.size();
    for (int i = 0; i < myCases.size(); i++) {
      if (arguments.get(offset + i).isInstance(ConCallExpression.class)) {
        Constructor constructor = arguments.get(offset + i).cast(ConCallExpression.class).getDefinition();
        if (constructor == Prelude.LEFT && myCases.get(i).proj1 != null || constructor == Prelude.RIGHT && myCases.get(i).proj2 != null) {
          return false;
        }
      }
      if (!arguments.get(offset + i).isWHNF()) {
        return false;
      }
    }
    return myOtherwise == null || myOtherwise.isWHNF(arguments);
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    int offset = DependentLink.Helper.size(myParameters) - myCases.size();
    for (int i = 0; i < myCases.size(); i++) {
      if (arguments.get(offset + i).isInstance(ConCallExpression.class)) {
        Constructor constructor = arguments.get(offset + i).cast(ConCallExpression.class).getDefinition();
        if (constructor == Prelude.LEFT && myCases.get(i).proj1 != null || constructor == Prelude.RIGHT && myCases.get(i).proj2 != null) {
          return null;
        }
      }
    }

    Expression stuck = myOtherwise == null ? null : myOtherwise.getStuckExpression(arguments, expression);
    if (stuck != null && stuck.isInstance(ErrorExpression.class)) {
      return stuck;
    }

    InferenceReferenceExpression refStuck = stuck != null ? stuck.checkedCast(InferenceReferenceExpression.class) : null;
    while (refStuck != null && refStuck.getSubstExpression() != null) {
      refStuck = refStuck.getSubstExpression().checkedCast(InferenceReferenceExpression.class);
    }
    for (int i = 0; i < myCases.size(); i++) {
      stuck = arguments.get(offset + i).getStuckExpression();
      if (stuck != null) {
        if (stuck.isInstance(ErrorExpression.class)) {
          return stuck;
        }
        if (refStuck == null) {
          refStuck = stuck.checkedCast(InferenceReferenceExpression.class);
          while (refStuck != null && refStuck.getSubstExpression() != null) {
            refStuck = refStuck.getSubstExpression().checkedCast(InferenceReferenceExpression.class);
          }
        }
      }
    }

    return refStuck;
  }
}
