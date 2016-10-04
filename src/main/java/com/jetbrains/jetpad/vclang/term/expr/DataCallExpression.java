package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class DataCallExpression extends DefCallExpression {
  private final List<Expression> myArguments;

  public DataCallExpression(DataDefinition definition, List<Expression> arguments) {
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
  public DataDefinition getDefinition() {
    return (DataDefinition) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDataCall(this, params);
  }

  @Override
  public DataCallExpression toDataCall() {
    return this;
  }

  @Override
  public Expression addArgument(Expression argument) {
    assert myArguments.size() < DependentLink.Helper.size(getDefinition().getParameters());
    myArguments.add(argument);
    return this;
  }
}
