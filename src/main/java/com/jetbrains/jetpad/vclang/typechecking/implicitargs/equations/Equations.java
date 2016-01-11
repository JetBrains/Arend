package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.List;

public interface Equations {
  void add(Equations equations);
  void add(Expression expr1, Expression expr2, CMP cmp);
  void abstractVar(int index, Expression type);
  void clear();
  boolean isEmpty();
  Equations newInstance();

  class Helper {
    public static void abstractVars(Equations equations, List<Binding> context, int from, int vars) {
      assert from + vars <= context.size();
      for (int i = 0; i < vars; i++) {
        equations.abstractVar(from, context.get(context.size() - 1 - i).getType());
      }
    }
  }

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
