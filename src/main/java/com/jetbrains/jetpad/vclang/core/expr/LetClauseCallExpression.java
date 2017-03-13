package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

import java.util.List;

public class LetClauseCallExpression extends Expression {
  private final LetClause myLetClause;
  private final List<Expression> myArguments;

  public LetClauseCallExpression(LetClause letClause, List<Expression> arguments) {
    myLetClause = letClause;
    myArguments = arguments;
  }

  public LetClause getLetClause() {
    return myLetClause;
  }

  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLetClauseCall(this, params);
  }

  @Override
  public LetClauseCallExpression toLetClauseCall() {
    return this;
  }
}
