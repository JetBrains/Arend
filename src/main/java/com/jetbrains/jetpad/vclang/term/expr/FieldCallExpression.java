package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;

public class FieldCallExpression extends DefCallExpression {
  private Expression myExpression;

  public FieldCallExpression(ClassField definition, Expression expression) {
    super(definition, new LevelSubstitution());
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
  public Expression applyThis(Expression thisExpr) {
    assert myExpression == null;
    return FieldCall(getDefinition(), thisExpr);
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
  public FieldCallExpression toFieldCall() {
    return this;
  }
}
