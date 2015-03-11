package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Position;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.DelegateProperty;
import jetbrains.jetpad.model.property.Property;

public class ParensExpression extends Expression {
  private final ChildProperty<ParensExpression, Expression> myExpression = new ChildProperty<>(this);

  public Expression getExpression() {
    return myExpression.get();
  }

  public Property<Expression> expression() {
    return new DelegateProperty<Expression>(myExpression) {
      @Override
      public void set(Expression expr) {
        ParensExpression.this.setExpression(expr);
      }
    };
  }

  public void setExpression(Expression expr) {
    myExpression.set(expr);
    if (expr != null) {
      expr.position = Position.PARENS;
    }
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return myExpression.get().accept(visitor, params);
  }

  public static Expression parens(boolean p, Expression expr) {
    if (p) {
      ParensExpression pexpr = new ParensExpression();
      pexpr.position = expr.position;
      pexpr.setExpression(expr);
      return pexpr;
    } else {
      return expr;
    }
  }
}
