package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class DummyEquations implements Equations {
  private static final DummyEquations INSTANCE = new DummyEquations();

  private DummyEquations() {}

  public static DummyEquations getInstance() {
    return INSTANCE;
  }

  @Override
  public void add(Equations equations) {
    if (!equations.isEmpty()) {
      throw new Exception();
    }
  }

  @Override
  public void add(Expression expr1, Expression expr2, CMP cmp) {
    throw new Exception();
  }

  @Override
  public void clear() {

  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public void abstractBinding(Binding binding) {

  }

  @Override
  public DummyEquations newInstance() {
    return this;
  }

  public static class Exception extends RuntimeException {

  }
}
