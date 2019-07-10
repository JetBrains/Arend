package org.arend.core.expr;

import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.SubstVisitor;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.util.Decision;

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
  public UniverseExpression subst(SubstVisitor substVisitor) {
    return new UniverseExpression(mySort.subst(substVisitor.getLevelSubstitution()));
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
  public Decision isWHNF(boolean normalizing) {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression(boolean normalizing) {
    return null;
  }
}
