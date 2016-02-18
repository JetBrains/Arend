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
    while (expression instanceof  LetExpression) {
      clauses.addAll(((LetExpression) expression).getClauses());
      expression = ((LetExpression) expression).getExpression();
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
    return Let(myClauses, myExpression.getType());
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }
}
