package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.Collections;
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
    myParameters = parameters;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    // TODO
    return this;
  }

  @Override
  public Constructor getDefinition() {
    return (Constructor) super.getDefinition();
  }

  @Override
  public Expression getType(List<Binding> context) {
    Expression resultType = super.getType(context);

    if (myParameters != null && !myParameters.isEmpty()) {
      List<Expression> parameters = new ArrayList<>(myParameters);
      Collections.reverse(parameters);
      resultType = resultType.subst(parameters, 0);
    }
    return resultType;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitConCall(this);
  }
}
