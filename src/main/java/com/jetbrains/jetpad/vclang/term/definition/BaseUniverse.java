package com.jetbrains.jetpad.vclang.term.definition;

public class BaseUniverse<L extends Universe.Level<L>> implements Universe  {
  private L myLevel;

  public BaseUniverse(L level) { myLevel = level; }

  public L getLevel() { return myLevel; }

  @Override
  public CompareResult compare(Universe other) {
    if (getClass() != other.getClass()) return new NotComparableResult();
    L otherLevel = ((BaseUniverse<L>) other).getLevel();
    Cmp res = myLevel.compare(otherLevel);
    if (res == Cmp.NOT_COMPARABLE) return new NotComparableResult();
    if (res == Cmp.UNKNOWN) return new OkCompareResult(new BaseUniverse<>(myLevel.max(otherLevel)), res);
    if (res == Cmp.LESS) return new OkCompareResult(other, res);
    return new OkCompareResult(this, res);
  }

  @Override
  public Universe succ() {
    return new BaseUniverse<>(myLevel.succ());
  }
}
