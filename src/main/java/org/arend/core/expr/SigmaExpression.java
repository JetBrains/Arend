package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.SubstVisitor;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.util.Decision;

public class SigmaExpression extends Expression implements Type {
  private final DependentLink myLink;
  private final Sort mySort;

  public SigmaExpression(Sort sort, DependentLink link) {
    assert link != null;
    myLink = link;
    mySort = sort;
  }

  public DependentLink getParameters() {
    return myLink;
  }

  public Sort getSort() {
    return mySort;
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return mySort;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitSigma(this, params);
  }

  @Override
  public SigmaExpression subst(SubstVisitor substVisitor) {
    return substVisitor.visitSigma(this, null);
  }

  @Override
  public SigmaExpression strip(LocalErrorReporter errorReporter) {
    return new StripVisitor(errorReporter).visitSigma(this, null);
  }

  @Override
  public SigmaExpression normalize(NormalizeVisitor.Mode mode) {
    return NormalizeVisitor.INSTANCE.visitSigma(this, mode);
  }

  @Override
  public Decision isWHNF(boolean normalizing) {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
