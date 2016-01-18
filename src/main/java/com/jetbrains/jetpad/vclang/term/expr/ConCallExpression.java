package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class ConCallExpression extends DefCallExpression {
  private List<Expression> myDataTypeArguments;

  public ConCallExpression(Constructor definition, List<Expression> dataTypeArguments) {
    super(definition);
    assert dataTypeArguments != null;
    myDataTypeArguments = dataTypeArguments;
  }

  public List<Expression> getDataTypeArguments() {
    return myDataTypeArguments;
  }

  public void setDataTypeArguments(List<Expression> parameters) {
    assert parameters != null;
    myDataTypeArguments = parameters;
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
  public Expression getType() {
    Expression resultType = super.getType();

    if (!myDataTypeArguments.isEmpty()) {
      resultType = resultType.subst(Utils.matchParameters(getDefinition().getDataTypeParameters(), myDataTypeArguments));
    }
    // TODO: add pi if there are not enough parameters
    return resultType;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }
}
