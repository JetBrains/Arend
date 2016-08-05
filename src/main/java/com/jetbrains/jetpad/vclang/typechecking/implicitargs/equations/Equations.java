package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

public interface Equations {
  boolean add(Equations equations);
  boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode);
  boolean add(Level level1, Level level2, CMP cmp, Abstract.SourceNode sourceNode);
  boolean add(Type type, Expression expr, Abstract.SourceNode sourceNode);
  boolean addVariable(LevelInferenceVariable var);
  void clear();
  boolean isEmpty();
  Equations newInstance();
  LevelSubstitution solve();
  void reportErrors(ErrorReporter errorReporter, Abstract.SourceNode sourceNode);

  enum CMP {
    LE, EQ, GE;

    public CMP not() {
      if (this == LE) return GE;
      if (this == GE) return LE;
      return this;
    }
  }
}
