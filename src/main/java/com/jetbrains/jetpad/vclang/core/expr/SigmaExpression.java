package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.StripVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

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
  public SigmaExpression subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    return new SubstVisitor(exprSubstitution, levelSubstitution).visitSigma(this, null);
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
  public boolean isWHNF() {
    return true;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
