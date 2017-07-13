package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

public class LetClauseCallExpression extends Expression {
  private final LetClause myLetClause;

  public LetClauseCallExpression(LetClause letClause) {
    myLetClause = letClause;
  }

  public LetClause getLetClause() {
    return myLetClause;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLetClauseCall(this, params);
  }

  @Override
  public LetClauseCallExpression toLetClauseCall() {
    return this;
  }

  public LetClause getDefinition() {
    return myLetClause;
  }
}
