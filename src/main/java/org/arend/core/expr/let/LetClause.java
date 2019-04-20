package org.arend.core.expr.let;

import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.NamedBinding;
import org.arend.core.expr.Expression;
import org.arend.core.subst.SubstVisitor;

public class LetClause extends NamedBinding implements EvaluatingBinding {
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

  public void setPattern(LetClausePattern pattern) {
    myPattern = pattern;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public void subst(SubstVisitor visitor) {
    myExpression = myExpression.accept(visitor, null);
  }

  public void setExpression(Expression expression) {
    myExpression = expression;
  }

  @Override
  public Expression getTypeExpr() {
    return myExpression.getType();
  }
}
