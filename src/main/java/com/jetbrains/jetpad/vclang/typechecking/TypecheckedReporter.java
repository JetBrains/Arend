package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface TypecheckedReporter {
  void typecheckingSucceeded(Abstract.Definition definition);
  void typecheckingFailed(Abstract.Definition definition);
}
