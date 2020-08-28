package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.pattern.Pattern;
import org.arend.ext.core.body.CoreElimClause;
import org.arend.ext.core.expr.AbstractedExpression;
import org.arend.extImpl.AbstractedExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElimClause<P extends Pattern> implements CoreElimClause {
  private final List<P> myPatterns;
  private Expression myExpression;

  public ElimClause(List<P> patterns, Expression expression) {
    myPatterns = patterns;
    myExpression = expression;
  }

  public DependentLink getParameters() {
    return Pattern.getFirstBinding(myPatterns);
  }

  @NotNull
  @Override
  public List<P> getPatterns() {
    return myPatterns;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  public void setExpression(Expression expr) {
    myExpression = expr;
  }

  @Override
  public @Nullable AbstractedExpression getAbstractedExpression() {
    return myExpression == null ? null : AbstractedExpressionImpl.make(getParameters(), myExpression);
  }
}
