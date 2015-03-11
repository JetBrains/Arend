package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Position;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class LamExpression extends Expression implements Abstract.LamExpression {
  private final Property<String> myVariable = new ValueProperty<>();
  private final ChildProperty<LamExpression, Expression> myBody = new ChildProperty<>(this);

  @Override
  public String getVariable() {
    return myVariable.get();
  }

  @Override
  public Expression getBody() {
    return myBody.get();
  }

  public Property<String> variable() {
    return myVariable;
  }

  public Property<Expression> body() {
    return new ValueProperty<Expression>(myBody.get()) {
      @Override
      public void set(Expression body) {
        LamExpression.this.setBody(body);
      }
    };
  }

  public void setVariable(String variable) {
    myVariable.set(variable);
  }

  public void setBody(Expression body) {
    myBody.set(body);
    if (body != null) {
      body.position = Position.LAM;
    }
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }
}
