package org.arend.core.expr;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.sort.Sort;
import org.arend.ext.core.expr.CoreLamExpression;
import org.arend.util.Decision;

import javax.annotation.Nonnull;

public class LamExpression extends Expression implements CoreLamExpression {
  private final Sort myResultSort;
  private final SingleDependentLink myLink;
  private final Expression myBody;

  public LamExpression(Sort resultSort, SingleDependentLink link, Expression body) {
    myResultSort = resultSort;
    myLink = link;
    myBody = body;
  }

  public Sort getResultSort() {
    return myResultSort;
  }

  @Nonnull
  @Override
  public SingleDependentLink getParameters() {
    return myLink;
  }

  @Nonnull
  @Override
  public Expression getBody() {
    return myBody;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }

  public Expression substArgument(Expression argument) {
    SingleDependentLink link = myLink.getNext();
    Expression body = myBody.subst(myLink, argument);
    return link.hasNext() ? new LamExpression(myResultSort, link, body) : body;
  }
}
