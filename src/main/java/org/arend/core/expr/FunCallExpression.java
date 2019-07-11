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
  public Decision isWHNF() {
    FunctionDefinition definition = getDefinition();
    if (definition == Prelude.COERCE || definition == Prelude.COERCE2) {
      return definition.getBody().isWHNF(myArguments).min(Decision.MAYBE);
    } else {
      return definition.getBody() != null ? definition.getBody().isWHNF(myArguments) : Decision.YES;
    }
  }

  @Override
  public Expression getStuckExpression() {
    FunctionDefinition definition = getDefinition();
    if (definition == Prelude.COERCE) {
      Expression stuck = myArguments.get(2).getStuckExpression();
      return stuck != null ? stuck : myArguments.get(0).getStuckExpression();
    }
    if (definition == Prelude.COERCE2) {
      Expression stuck = myArguments.get(1).getStuckExpression();
      if (stuck != null) {
        return stuck;
      }
      stuck = myArguments.get(3).getStuckExpression();
      return stuck != null ? stuck : myArguments.get(0).getStuckExpression();
    }
    return definition.getBody() != null ? definition.getBody().getStuckExpression(myArguments, this) : null;
  }
}
