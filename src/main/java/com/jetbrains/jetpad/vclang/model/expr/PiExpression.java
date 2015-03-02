package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class PiExpression extends Expression implements Abstract.PiExpression {
  private final Property<Boolean> myExplicit = new ValueProperty<>();
  private final Property<String> myVariable = new ValueProperty<>();
  private final Property<Expression> myDomain = new ValueProperty<>();
  private final Property<Expression> myCodomain = new ValueProperty<>();

  @Override
  public boolean isExplicit() {
    return myExplicit.get();
  }

  @Override
  public String getVariable() {
    return myVariable.get();
  }

  @Override
  public Abstract.Expression getDomain() {
    return myDomain.get();
  }

  @Override
  public Abstract.Expression getCodomain() {
    return myCodomain.get();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }
}
