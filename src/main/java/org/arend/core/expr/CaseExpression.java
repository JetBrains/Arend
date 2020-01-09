package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.ext.core.expr.CoreCaseExpression;
import org.arend.util.Decision;

import javax.annotation.Nonnull;
import java.util.List;

public class CaseExpression extends Expression implements CoreCaseExpression {
  private final boolean mySFunc;
  private final DependentLink myParameters;
  private final Expression myResultType;
  private final Expression myResultTypeLevel;
  private final ElimTree myElimTree;
  private final List<Expression> myArguments;

  public CaseExpression(boolean isSFunc, DependentLink parameters, Expression resultType, Expression resultTypeLevel, ElimTree elimTree, List<Expression> arguments) {
    mySFunc = isSFunc;
    myParameters = parameters;
    myElimTree = elimTree;
    myResultType = resultType;
    myResultTypeLevel = resultTypeLevel;
    myArguments = arguments;
  }

  @Override
  public boolean isSCase() {
    return mySFunc;
  }

  @Nonnull
  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  @Nonnull
  @Override
  public Expression getResultType() {
    return myResultType;
  }

  @Override
  public Expression getResultTypeLevel() {
    return myResultTypeLevel;
  }

  @Nonnull
  @Override
  public ElimTree getElimTree() {
    return myElimTree;
  }

  @Nonnull
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
  public Decision isWHNF() {
    return myElimTree.isWHNF(myArguments);
  }

  @Override
  public Expression getStuckExpression() {
    return myElimTree.getStuckExpression(myArguments, this);
  }
}
