package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class ConCallExpression extends DefCallExpression {
  private List<Expression> myParameters;

  public ConCallExpression(Constructor definition, List<Expression> parameters) {
    super(definition);
    assert parameters != null;
    myParameters = parameters;
  }

  public List<Expression> getParameters() {
    return myParameters;
  }

  public void setParameters(List<Expression> parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    assert myParameters.isEmpty();
    myParameters.add(thisExpr);
    return this;
  }

  @Override
  public Constructor getDefinition() {
    return (Constructor) super.getDefinition();
  }

  @Override
  public Expression getType() {
    Expression resultType = super.getType();

    if (!myParameters.isEmpty()) {
      resultType = resultType.subst(Utils.matchParameters(getDefinition().getDataType().getParameters(), myParameters));
    }
    return resultType;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }
}
