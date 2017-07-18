package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.Collections;
import java.util.List;

public class FieldCallExpression extends DefCallExpression {
  private final Expression myExpression;

  public FieldCallExpression(ClassField definition, Expression expression) {
    super(definition);
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return Collections.singletonList(myExpression);
  }

  @Override
  public Sort getSortArgument() {
    return Sort.PROP;
  }

  @Override
  public ClassField getDefinition() {
    return (ClassField) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFieldCall(this, params);
  }

  @Override
  public boolean isWHNF() {
    return false;
  }

  @Override
  public Expression getStuckExpression() {
    return myExpression.getStuckExpression();
  }
}
