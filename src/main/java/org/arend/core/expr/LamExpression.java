package org.arend.core.expr;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreLamExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class LamExpression extends Expression implements CoreLamExpression {
  private Sort myResultSort;
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

  public void substSort(LevelSubstitution substitution) {
    myResultSort = myResultSort.subst(substitution);
  }

  @NotNull
  @Override
  public SingleDependentLink getParameters() {
    return myLink;
  }

  @NotNull
  @Override
  public Expression getBody() {
    return myBody;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitLam(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
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
