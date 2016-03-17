package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public class DummyTypecheckedReported implements TypecheckedReporter {
  @Override
  public void typecheckingSucceeded(Abstract.Definition abstractDefinition, Definition definition) {

  }

  @Override
  public void typecheckingFailed(Abstract.Definition definition) {

  }
}
