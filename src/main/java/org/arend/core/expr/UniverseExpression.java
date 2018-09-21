package org.arend.core.expr;

import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.typechecking.error.LocalErrorReporter;

public class UniverseExpression extends Expression implements Type {
  private final Sort mySort;

  public UniverseExpression(Sort sort) {
    assert !sort.isOmega();
    mySort = sort;
  }

  public Sort getSort() {
    return mySort;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitUniverse(this, params);
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return mySort.succ();
  }

  @Override
  public UniverseExpression subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    return new UniverseExpression(mySort.subst(levelSubstitution));
  }

  @Override
  public UniverseExpression strip(LocalErrorReporter errorReporter) {
    return this;
  }

  @Override
  public UniverseExpression normalize(NormalizeVisitor.Mode mode) {
    return this;
  }

  @Override
  public boolean isWHNF() {
    return true;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
