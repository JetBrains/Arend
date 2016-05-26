package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class ConCallExpression extends DefCallExpression {
  private List<Expression> myDataTypeArguments;

  public ConCallExpression(Constructor definition, List<Expression> dataTypeArguments) {
    super(definition);
    assert dataTypeArguments != null;
    // TODO: constructors
    // assert dataTypeArguments.size() == DependentLink.Helper.size(definition.getDataTypeParameters());
    myDataTypeArguments = dataTypeArguments;
  }

  public List<? extends Expression> getDataTypeArguments() {
    return myDataTypeArguments;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    /* TODO: constructors
    assert !myDataTypeArguments.isEmpty() && myDataTypeArguments.get(0) == null;
    myDataTypeArguments.set(0, thisExpr);
    */
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
    return getDefinition().getType().applyExpressions(myDataTypeArguments);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }

  @Override
  public ConCallExpression toConCall() {
    return this;
  }
}
