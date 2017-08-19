package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface Equations<T> {
  boolean add(Expression expr1, Expression expr2, CMP cmp, Concrete.SourceNode<T> sourceNode, InferenceVariable<T> stuckVar);
  boolean solve(Expression type, Expression expr, CMP cmp, Concrete.SourceNode<T> sourceNode);
  boolean add(Level level1, Level level2, CMP cmp, Concrete.SourceNode<T> sourceNode);
  boolean addVariable(InferenceLevelVariable<T> var);
  void bindVariables(InferenceLevelVariable<T> pVar, InferenceLevelVariable<T> hVar);
  void remove(Equation<T> equation);
  LevelSubstitution solve(Concrete.SourceNode<T> sourceNode);

  enum CMP {
    LE, EQ, GE;

    public CMP not() {
      if (this == LE) return GE;
      if (this == GE) return LE;
      return this;
    }
  }
}
