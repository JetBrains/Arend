package com.jetbrains.jetpad.vclang.term.definition;

public interface Universe {
  enum Cmp { EQUALS, UNKNOWN, LESS, GREATER, NOT_COMPARABLE }

  interface Level<L> {
    Cmp compare(L other);
    L max(L other);
    L succ();
  }

  interface LeveledUniverseFactory<U extends Universe, L extends Level<L>> {
    U createUniverse(L level);
  }

  class CompareResult {
    public Universe MaxUniverse;
    public Cmp Result;

    public CompareResult(Universe maxUniverse, Cmp result) {
      MaxUniverse = maxUniverse;
      Result = result;
    }

    public boolean isLessOrEquals() { return Result == Cmp.LESS || Result == Cmp.EQUALS; }
  }

  CompareResult compare(Universe other);
  Universe succ();
}
