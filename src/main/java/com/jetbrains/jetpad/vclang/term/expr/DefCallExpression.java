package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class DefCallExpression extends Expression implements Abstract.DefCallExpression {
  private final Expression myExpression;
  private final Definition myDefinition;

  public DefCallExpression(Expression expression, Definition definition) {
    myDefinition = definition;
    myExpression = expression;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public String getName() {
    return myDefinition.getName();
  }

  @Override
  public Abstract.Definition.Fixity getFixity() {
    return myDefinition.getFixity();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitDefCall(this);
  }

  @Override
  public Expression getType(List<Expression> context) {
    return myDefinition.getType();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDefCall(this, params);
  }
}
