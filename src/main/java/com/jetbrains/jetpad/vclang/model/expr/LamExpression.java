package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class LamExpression extends Expression implements Abstract.LamExpression {
  public final Property<String> variable = new ValueProperty<>();
  public final ChildProperty<LamExpression, Expression> body = new ChildProperty<>(this);

  @Override
  public String getVariable() {
    return variable.get();
  }

  @Override
  public Abstract.Expression getBody() {
    return body.get();
  }
  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }
}
