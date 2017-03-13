package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;

import java.util.List;

public class DataCallExpression extends DefCallExpression {
  private final LevelArguments myLevelArguments;
  private final List<Expression> myArguments;

  public DataCallExpression(DataDefinition definition, LevelArguments levelArguments, List<Expression> arguments) {
    super(definition);
    assert definition.status().headerIsOK();
    myLevelArguments = levelArguments;
    myArguments = arguments;
  }

  @Override
  public LevelArguments getLevelArguments() {
    return myLevelArguments;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
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
