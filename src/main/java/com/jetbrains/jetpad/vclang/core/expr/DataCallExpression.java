package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

public class DataCallExpression extends DefCallExpression {
  private final List<Expression> myArguments;

  public DataCallExpression(DataDefinition definition, LevelArguments polyParams, List<Expression> arguments) {
    super(definition, polyParams);
    myArguments = arguments;

    if (polyParams.getLevels().size() < definition.getPolyParams().size()) {
      List<Level> extendedPolyParams = new ArrayList<>(polyParams.getLevels());
      for (int i = polyParams.getLevels().size(); i < definition.getPolyParams().size(); ++i) {
        extendedPolyParams.add(Level.INFINITY);
      }
      setPolyParamsSubst(new LevelArguments(extendedPolyParams));
    } /**/
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    myArguments.add(thisExpr);
    return this;
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
