package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.List;

public interface Equations {
  void add(Equations equations);
  void add(Expression expr1, Expression expr2, CMP cmp);
  void clear();
  boolean isEmpty();
  void abstractBinding(Binding binding);
  Equations newInstance();
  Substitution getInferenceVariables(List<InferenceBinding> binding);
  void reportErrors(ErrorReporter errorReporter);

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
