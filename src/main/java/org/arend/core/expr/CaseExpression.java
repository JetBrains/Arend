package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreCaseExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CaseExpression extends Expression implements CoreCaseExpression {
  private final boolean mySFunc;
  private final DependentLink myParameters;
  private final Expression myResultType;
  private final Expression myResultTypeLevel;
  private final ElimBody myElimBody;
  private final List<Expression> myArguments;

  public CaseExpression(boolean isSFunc, DependentLink parameters, Expression resultType, Expression resultTypeLevel, ElimBody elimBody, List<Expression> arguments) {
    mySFunc = isSFunc;
    myParameters = parameters;
    myElimBody = elimBody;
    myResultType = resultType;
    myResultTypeLevel = resultTypeLevel;
    myArguments = arguments;
  }

  @Override
  public boolean isSCase() {
    return mySFunc;
  }

  @NotNull
  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  @NotNull
  @Override
  public Expression getResultType() {
    return myResultType;
  }

  @Override
  public Expression getResultTypeLevel() {
    return myResultTypeLevel;
  }

  @NotNull
  @Override
  public ElimBody getElimBody() {
    return myElimBody;
  }

  @NotNull
  @Override
  public List<Expression> getArguments() {
    return myArguments;
  }

  @Override
  public boolean canBeConstructor() {
    if (mySFunc) {
      return false;
    }
    for (Expression argument : myArguments) {
      if (argument.canBeConstructor()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitCase(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitCase(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitCase(this, params);
  }

  @Override
  public Decision isWHNF() {
    return myElimBody.isWHNF(myArguments);
  }

  @Override
  public Expression getStuckExpression() {
    return myElimBody.getStuckExpression(myArguments, this);
  }
}
