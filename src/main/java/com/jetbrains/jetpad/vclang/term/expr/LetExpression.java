package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Let;

public class LetExpression extends Expression {
  private final List<LetClause> myClauses;
  private final Expression myExpression;

  public LetExpression(List<LetClause> clauses, Expression expression) {
    myClauses = clauses;
    myExpression = expression;
  }

  public LetExpression mergeNestedLets() {
    List<LetClause> clauses = new ArrayList<>(myClauses);
    Expression expression = myExpression;
    while (expression.toLet() != null) {
      clauses.addAll(expression.toLet().getClauses());
      expression = expression.toLet().getExpression();
    }
    return Let(clauses, expression);
  }

  public List<LetClause> getClauses() {
    return myClauses;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Expression getType() {
    Expression type = myExpression.getType();
    return type != null ? Let(myClauses, type) : null;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }

  @Override
  public LetExpression toLet() {
    return this;
  }
}
