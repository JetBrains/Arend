package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.Collections;
import java.util.List;

public class ElimExpression extends Expression implements Abstract.ElimExpression {
  private final List<IndexExpression> myExpressions;
  private final List<Clause> myClauses;

  public ElimExpression(IndexExpression expression, List<Clause> clauses) {
    this(Collections.singletonList(expression), clauses);
  }

  public ElimExpression(List<IndexExpression> expression, List<Clause> clauses) {
    myExpressions = expression;
    myClauses = clauses;
  }

  @Override
  public List<IndexExpression> getExpressions() {
    return myExpressions;
  }

  @Override
  public List<Clause> getClauses() {
    return myClauses;
  }

  @Override
  public Expression getType(List<Binding> context) {
    return null;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitElim(this, params);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitElim(this, params);
  }
}
