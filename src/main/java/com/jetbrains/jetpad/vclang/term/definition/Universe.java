package com.jetbrains.jetpad.vclang.term.definition;

public interface Universe {
  enum Cmp { EQUALS, UNKNOWN, LESS, GREATER, NOT_COMPARABLE }

  interface Level<L> {
    Cmp compare(L other);
    L max(L other);
    L succ();
  }

  interface CompareResult {}

  class OkCompareResult implements CompareResult {
    public Universe myMaxUniverse;
    public Cmp myResult;

    public OkCompareResult(Universe maxUniverse, Cmp result) {
      myMaxUniverse = maxUniverse;
      myResult = result;
    }
  }

  class NotComparableResult implements CompareResult {}

  CompareResult compare(Universe other);
  Universe succ();
}
