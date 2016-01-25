package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public interface Equations {
  void add(Equations equations);
  void add(Expression expr1, Expression expr2, CMP cmp);
  void clear();
  boolean isEmpty();
  Equations newInstance();

  enum CMP {
    LE, EQ, GE;

    public Universe.Cmp toUniverseCmp() {
      switch (this) {
        case LE: return Universe.Cmp.LESS;
        case EQ: return Universe.Cmp.EQUALS;
        case GE: return Universe.Cmp.GREATER;
      }
      throw new IllegalStateException();
    }

    public CMP not() {
      if (this == LE) return GE;
      if (this == GE) return LE;
      return this;
    }
  }
}
