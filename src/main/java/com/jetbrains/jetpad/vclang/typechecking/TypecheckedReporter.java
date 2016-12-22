package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface TypecheckedReporter {
  void typecheckingSucceeded(Abstract.Definition definition);
  void typecheckingFailed(Abstract.Definition definition);


  class Dummy implements TypecheckedReporter {
    @Override
    public void typecheckingSucceeded(Abstract.Definition definition) {
    }

    @Override
    public void typecheckingFailed(Abstract.Definition definition) {
    }
  }
}
