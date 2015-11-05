package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public class DummyTypecheckedReported implements TypecheckedReporter {
  @Override
  public void typecheckingSucceeded(ResolvedName definitionName) {

  }

  @Override
  public void typecheckingFailed(ResolvedName definitionName) {

  }
}
