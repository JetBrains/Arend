package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

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
  public boolean isWHNF() {
    //noinspection SimplifiableConditionalExpression
    return getDefinition().getBody() != null ? getDefinition().getBody().isWHNF(myArguments) : true;
  }

  @Override
  public Expression getStuckExpression() {
    return getDefinition().getBody() != null ? getDefinition().getBody().getStuckExpression(myArguments, this) : null;
  }
}
