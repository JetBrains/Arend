package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

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
    DependentLink parameters = getDefinition().getDataTypeParameters();
    Expression result = getDefinition().getType();
    if (parameters.hasNext()) {
      result = Pi(parameters, result);
    }
    return result.applyExpressions(myDataTypeArguments);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }
}
