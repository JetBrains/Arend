package org.arend.core.expr;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.sort.Sort;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;

import java.util.List;

public class FunCallExpression extends DefCallExpression {
  private final Sort mySortArgument;
  private final List<Expression> myArguments;

  public FunCallExpression(FunctionDefinition definition, Sort sortArgument, List<Expression> arguments) {
    super(definition);
    mySortArgument = sortArgument;
    myArguments = arguments;
  }

  @Override
  public Sort getSortArgument() {
    return mySortArgument;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public FunctionDefinition getDefinition() {
    return (FunctionDefinition) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunCall(this, params);
  }

  @Override
  public Decision isWHNF(boolean normalizing) {
    return getDefinition().getBody() != null ? getDefinition().getBody().isWHNF(myArguments, normalizing) : Decision.YES;
  }

  @Override
  public Expression getStuckExpression(boolean normalizing) {
    if (getDefinition() == Prelude.COERCE) {
      Expression stuck = myArguments.get(2).getStuckExpression(normalizing);
      return stuck != null ? stuck : myArguments.get(0).getStuckExpression(normalizing);
    }
    if (getDefinition() == Prelude.COERCE2) {
      Expression stuck = myArguments.get(1).getStuckExpression(normalizing);
      if (stuck != null) {
        return stuck;
      }
      stuck = myArguments.get(3).getStuckExpression(normalizing);
      return stuck != null ? stuck : myArguments.get(0).getStuckExpression(normalizing);
    }
    return getDefinition().getBody() != null ? getDefinition().getBody().getStuckExpression(myArguments, this, normalizing) : null;
  }
}
