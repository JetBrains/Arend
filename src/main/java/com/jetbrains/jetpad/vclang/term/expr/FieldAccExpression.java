package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class FieldAccExpression extends Expression implements Abstract.FieldAccExpression {
  private final Expression myExpression;
  private final Definition myField;

  public FieldAccExpression(Expression expression, Definition field) {
    myExpression = expression;
    myField = field;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public String getName() {
    return myField.getName();
  }

  @Override
  public Abstract.Definition.Fixity getFixity() {
    return myField.getFixity();
  }

  @Override
  public Definition getField() {
    return myField;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitFieldAcc(this);
  }

  @Override
  public Expression getType(List<Expression> context) {
    return myField.getType();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFieldAcc(this, params);
  }
}
