package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.subst.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

import java.util.Set;

public interface Equations {
  boolean add(Equations equations);
  boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode);
  boolean add(Level level1, Level level2, CMP cmp, Abstract.SourceNode sourceNode);
  boolean add(Type type, Expression expr, Abstract.SourceNode sourceNode);
  boolean addVariable(LevelInferenceVariable var);
  void clear();
  boolean isEmpty();
  void abstractBinding(Binding binding);
  Equations newInstance();
  Substitution solve(Set<InferenceVariable> bindings, boolean isFinal);
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
