package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class FieldAccExpression extends Expression implements Abstract.FieldAccExpression {
  private final Expression myExpression;
  private final Definition myDefinition;
  private final int myIndex;

  public FieldAccExpression(Expression expression, Definition definition, int index) {
    myExpression = expression;
    myDefinition = definition;
    myIndex = index;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public String getName() {
    return myDefinition == null ? "proj" + myIndex : myDefinition.getName();
  }

  @Override
  public Abstract.Definition.Fixity getFixity() {
    return myDefinition == null ? Abstract.Definition.Fixity.PREFIX : myDefinition.getFixity();
  }

  @Override
  public Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public int getIndex() {
    return myIndex;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitFieldAcc(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFieldAcc(this, params);
  }
}
