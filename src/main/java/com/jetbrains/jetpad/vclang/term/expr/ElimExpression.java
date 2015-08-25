package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class ElimExpression extends Expression implements Abstract.ElimExpression {
  private final IndexExpression myExpression;
  private final List<Clause> myClauses;

  public ElimExpression(IndexExpression expression, List<Clause> clauses) {
    myExpression = expression;
    myClauses = clauses;
  }

  @Override
  public IndexExpression getExpression() {
    return myExpression;
  }

  @Override
  public List<Clause> getClauses() {
    return myClauses;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitElim(this);
  }

  @Override
  public Expression getType(List<Binding> context) {
    return null;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitElim(this, params);
  }
}
