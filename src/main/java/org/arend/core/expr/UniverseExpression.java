package org.arend.core.expr;

import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.ErrorReporter;
import org.arend.ext.core.expr.CoreUniverseExpression;
import org.arend.util.Decision;

import javax.annotation.Nonnull;

public class UniverseExpression extends Expression implements Type, CoreUniverseExpression {
  private final Sort mySort;

  public UniverseExpression(Sort sort) {
    assert !sort.isOmega();
    mySort = sort;
  }

  @Nonnull
  @Override
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
    return substVisitor.getLevelSubstitution().isEmpty() ? this : new UniverseExpression(mySort.subst(substVisitor.getLevelSubstitution()));
  }

  @Override
  public UniverseExpression strip(ErrorReporter errorReporter) {
    return this;
  }

  @Override
  public UniverseExpression normalize(NormalizeVisitor.Mode mode) {
    return this;
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
