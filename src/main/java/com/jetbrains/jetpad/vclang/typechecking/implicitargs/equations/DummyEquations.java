package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

public class DummyEquations implements Equations {
  private static final DummyEquations INSTANCE = new DummyEquations();

  private DummyEquations() {}

  public static DummyEquations getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode, InferenceVariable stuckVar) {
    return false;
  }

  @Override
  public boolean solve(Type type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode) {
    return false;
  }

  @Override
  public boolean add(Level expr1, Level expr2, CMP cmp, Abstract.SourceNode sourceNode) {
    return false;
  }

  @Override
  public boolean add(Type type, Expression expr, Abstract.SourceNode sourceNode, InferenceVariable stuckVar) {
    return false;
  }

  @Override
  public boolean addVariable(LevelInferenceVariable var) {
    return false;
  }

  @Override
  public void remove(Equation equation) {

  }

  @Override
  public LevelSubstitution solve(Abstract.SourceNode sourceNode) {
    return new LevelSubstitution();
  }
}
