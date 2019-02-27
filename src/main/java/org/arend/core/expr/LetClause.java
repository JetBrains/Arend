package org.arend.core.expr;

import org.arend.core.context.binding.NamedBinding;

public class LetClause extends NamedBinding {
  private LetClausePattern myPattern;
  private Expression myExpression;

  public LetClause(String name, LetClausePattern pattern, Expression expression) {
    super(name);
    myPattern = pattern;
    myExpression = expression;
  }

  public LetClausePattern getPattern() {
    return myPattern;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public void setExpression(Expression expression) {
    myExpression = expression;
  }

  @Override
  public Expression getTypeExpr() {
    return myExpression.getType();
  }
}
