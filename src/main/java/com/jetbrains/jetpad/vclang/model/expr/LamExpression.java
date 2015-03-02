package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class LamExpression extends Expression implements Abstract.LamExpression {
  private final Property<String> myVariable = new ValueProperty<>();
  private final Property<Expression> myBody = new ValueProperty<>();

  @Override
  public String getVariable() {
    return myVariable.get();
  }

  @Override
  public Abstract.Expression getBody() {
    return myBody.get();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }
}
