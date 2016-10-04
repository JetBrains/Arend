package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class FunCallExpression extends DefCallExpression {
  private final List<Expression> myArguments;

  public FunCallExpression(FunctionDefinition definition, List<Expression> arguments) {
    super(definition);
    myArguments = arguments;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    return ExpressionFactory.Apps(this, thisExpr);
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
  public FunCallExpression toFunCall() {
    return this;
  }

  @Override
  public Expression addArgument(Expression argument) {
    if (myArguments.size() < DependentLink.Helper.size(getDefinition().getParameters())) {
      myArguments.add(argument);
      return this;
    } else {
      return super.addArgument(argument);
    }
  }
}
