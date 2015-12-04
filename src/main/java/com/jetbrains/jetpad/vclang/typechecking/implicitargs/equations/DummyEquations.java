package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class DummyEquations implements Equations {
  private static final DummyEquations INSTANCE = new DummyEquations();

  private DummyEquations() {}

  public static DummyEquations getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean lift(int on) {
    return true;
  }

  @Override
  public boolean add(Equations equations) {
    return equations.isEmpty();
  }

  @Override
  public boolean add(int var, Expression expr, CMP cmp) {
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
}
