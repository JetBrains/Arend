package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class DummyTypecheckedReported implements TypecheckedReporter {
  @Override
  public void typecheckingSucceeded(Abstract.Definition definition) {

  }

  @Override
  public void typecheckingFailed(Abstract.Definition definition) {

  }
}
