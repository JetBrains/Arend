package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.Set;

public interface Equations {
  boolean add(Equations equations);
  boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode);
  void clear();
  boolean isEmpty();
  void abstractBinding(Binding binding);
  Equations newInstance();
  Substitution getInferenceVariables(Set<InferenceBinding> binding, boolean onlyPreciseSolutions);
  void reportErrors(ErrorReporter errorReporter);

  enum CMP {
    LE, EQ, GE;

    public CMP not() {
      if (this == LE) return GE;
      if (this == GE) return LE;
      return this;
    }
  }
}
