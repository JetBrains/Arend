package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class AppExpression extends Expression implements Abstract.AppExpression {
  public final Property<Expression> function = new ValueProperty<>();
  public final Property<Expression> argument = new ValueProperty<>();

  @Override
  public Expression getFunction() {
    return function.get();
  }

  @Override
  public Expression getArgument() {
    return argument.get();
  }
  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }
}
