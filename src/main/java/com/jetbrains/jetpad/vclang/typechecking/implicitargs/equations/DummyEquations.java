package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
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
  public boolean add(Equations equations) {
    return equations.isEmpty();
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode) {
    return false;
  }

  @Override
  public boolean add(Level expr1, Level expr2, CMP cmp, Abstract.SourceNode sourceNode) {
    return false;
  }

  @Override
  public boolean add(Type type, Expression expr, Abstract.SourceNode sourceNode) {
    return false;
  }

  @Override
  public boolean addVariable(LevelInferenceVariable var) {
    return false;
  }

  @Override
  public void clear() {

  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public DummyEquations newInstance() {
    return this;
  }

  @Override
  public LevelSubstitution solve() {
    return new LevelSubstitution();
  }

  @Override
  public void reportErrors(ErrorReporter errorReporter, Abstract.SourceNode sourceNode) {

  }
}
