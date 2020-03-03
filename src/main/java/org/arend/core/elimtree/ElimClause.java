package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.pattern.Pattern;

import java.util.List;

public class ElimClause<P extends Pattern> {
  private final List<P> myPatterns;
  private Expression myExpression;

  public ElimClause(List<P> patterns, Expression expression) {
    myPatterns = patterns;
    myExpression = expression;
  }

  public DependentLink getParameters() {
    return Pattern.getFirstBinding(myPatterns);
  }

  public List<P> getPatterns() {
    return myPatterns;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public void setExpression(Expression expr) {
    myExpression = expr;
  }
}
