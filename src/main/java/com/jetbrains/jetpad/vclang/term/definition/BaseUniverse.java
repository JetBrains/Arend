package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;

public abstract class BaseUniverse<U extends Universe, L extends Universe.Level<L>> implements Universe, Universe.LeveledUniverseFactory<U, L>  {
  private L myLevel = null;

  public BaseUniverse() {}

  public BaseUniverse(L level) { myLevel = level; }

  public L getLevel() { return myLevel; }

  @Override
  public CompareResult compare(Universe other, CompareVisitor visitor) {
    if (getClass() != other.getClass()) return null;
    L otherLevel = ((BaseUniverse<U, L>) other).getLevel();
    if (myLevel == null)
      return otherLevel == null ? new CompareResult(this, Cmp.EQUALS) : new CompareResult(this, Cmp.GREATER);
    Cmp res = myLevel.compare(otherLevel, visitor);
    if (res == Cmp.UNKNOWN || res == Cmp.NOT_COMPARABLE)
      return new CompareResult(createUniverse(myLevel.max(otherLevel)), res);
    if (res == Cmp.LESS) return new CompareResult(other, res);
    return new CompareResult(this, res);
  }

  @Override
  public Universe succ() {
    return myLevel != null ? createUniverse(myLevel.succ()) : null;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Universe)) return false;
    CompareResult cmp = compare((Universe) other, null);
    return cmp != null && cmp.Result == Cmp.EQUALS;
  }

  protected static boolean compreLevelExprs(Expression expr1, Expression expr2, CompareVisitor visitor) {
    return visitor == null ? Expression.compare(expr1, expr2) : expr1.accept(visitor, expr2);
  }
}
