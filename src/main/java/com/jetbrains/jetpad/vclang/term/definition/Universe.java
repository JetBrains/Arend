package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public interface Universe {
  enum Cmp { EQUALS, LESS, GREATER, NOT_COMPARABLE }

  interface Level<L> {
    Cmp compare(L other);
    boolean compare(L other, CompareVisitor visitor, Equations.CMP expectedCMP);
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
  boolean compare(Universe other, CompareVisitor visitor, Equations.CMP expectedCMP);
  Universe succ();
}
