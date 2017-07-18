package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

public class LetExpression extends Expression {
  private final List<LetClause> myClauses;
  private final Expression myExpression;

  public LetExpression(List<LetClause> clauses, Expression expression) {
    myClauses = clauses;
    myExpression = expression;
  }

  // TODO[cmpNorm]: Remove this method
  public LetExpression mergeNestedLets() {
    List<LetClause> clauses = new ArrayList<>(myClauses);
    Expression expression = myExpression;
    while (expression.isInstance(LetExpression.class)) {
      clauses.addAll(expression.cast(LetExpression.class).getClauses());
      expression = expression.cast(LetExpression.class).getExpression();
    }
    return new LetExpression(clauses, expression);
  }

  public List<LetClause> getClauses() {
    return myClauses;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }

  @Override
  public Expression getStuckExpression() {
    return myExpression.getStuckExpression();
  }
}
