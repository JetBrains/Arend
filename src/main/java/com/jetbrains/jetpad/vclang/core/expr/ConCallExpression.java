package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

import java.util.Collection;
import java.util.List;

public class ConCallExpression extends DefCallExpression {
  private final List<Expression> myDataTypeArguments;
  private final List<Expression> myArguments;

  public ConCallExpression(Constructor definition, LevelArguments polyParams, List<Expression> dataTypeArguments, List<Expression> arguments) {
    super(definition, polyParams);
    assert dataTypeArguments != null;
    myDataTypeArguments = dataTypeArguments;
    myArguments = arguments;
  }

  public List<? extends Expression> getDataTypeArguments() {
    return myDataTypeArguments;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    assert myDataTypeArguments.isEmpty();
    myDataTypeArguments.add(thisExpr);
    return this;
  }

  @Override
  public Constructor getDefinition() {
    return (Constructor) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }

  @Override
  public ConCallExpression toConCall() {
    return this;
  }

  @Override
  public Expression addArgument(Expression argument) {
    if (myDataTypeArguments.size() < DependentLink.Helper.size(getDefinition().getDataTypeParameters())) {
      myDataTypeArguments.add(argument);
    } else {
      assert myArguments.size() < DependentLink.Helper.size(getDefinition().getParameters());
      myArguments.add(argument);
    }
    return this;
  }

  public void addDefCallArguments(Collection<? extends Expression> arguments) {
    myArguments.addAll(arguments);
    assert myArguments.size() <= DependentLink.Helper.size(getDefinition().getParameters());
  }
}
