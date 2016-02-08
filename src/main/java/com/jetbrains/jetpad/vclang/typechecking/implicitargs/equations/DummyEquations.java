package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.List;

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

  @Override
  public Substitution getInferenceVariables(List<InferenceBinding> binding) {
    throw new Exception();
  }

  @Override
  public void reportErrors(ErrorReporter errorReporter) {

  }

  public static class Exception extends RuntimeException {

  }
}
