package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;

public interface Equations {
  boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode, InferenceVariable stuckVar);
  boolean solve(TypeMax type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode);
  boolean add(Level level1, Level level2, CMP cmp, Abstract.SourceNode sourceNode);
  boolean add(TypeMax type, Expression expr, Abstract.SourceNode sourceNode, InferenceVariable stuckVar);
  boolean addVariable(InferenceLevelVariable var);
  void remove(Equation equation);
  LevelSubstitution solve(Abstract.SourceNode sourceNode);

  enum CMP {
    LE, EQ, GE;

    public CMP not() {
      if (this == LE) return GE;
      if (this == GE) return LE;
      return this;
    }
  }
}
