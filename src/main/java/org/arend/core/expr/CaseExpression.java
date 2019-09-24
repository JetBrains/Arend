package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.util.Decision;

import java.util.List;

public class CaseExpression extends Expression {
  private final DependentLink myParameters;
  private final Expression myResultType;
  private final Expression myResultTypeLevel;
  private final ElimBody myElimBody;
  private final List<Expression> myArguments;

  public CaseExpression(DependentLink parameters, Expression resultType, Expression resultTypeLevel, ElimBody elimBody, List<Expression> arguments) {
    myParameters = parameters;
    myElimBody = elimBody;
    myResultType = resultType;
    myResultTypeLevel = resultTypeLevel;
    myArguments = arguments;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public Expression getResultType() {
    return myResultType;
  }

  public Expression getResultTypeLevel() {
    return myResultTypeLevel;
  }

  public ElimTree getElimTree() {
    return myElimBody.getElimTree();
  }

  public ElimBody getElimBody() {
    return myElimBody;
  }

  public List<Expression> getArguments() {
    return myArguments;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
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
