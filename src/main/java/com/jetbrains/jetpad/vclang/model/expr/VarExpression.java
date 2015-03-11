package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class VarExpression extends Expression implements Abstract.VarExpression {
  private final Property<String> myName = new ValueProperty<>();

  @Override
  public String getName() {
    return myName.get();
  }

  public Property<String> name() {
    return myName;
  }

  public void setName(String name) {
    myName.set(name);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitVar(this, params);
  }
}
