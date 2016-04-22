package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public interface Universe {
  enum Cmp { EQUALS, LESS, GREATER, NOT_COMPARABLE }

  interface Level<L> {
    Cmp compare(L other);
    L max(L other);
    L succ();
  }

  interface LeveledUniverseFactory<U extends Universe, L extends Level<L>> {
    U createUniverse(L level);
  }

  /*
  interface Operations {
    Universe pi(List<Universe> domainUni, Universe codomain);
    Universe sigma(List<Universe> domainUni);
    Universe dataType();
  } /**/

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
