package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface TypecheckedReporter {
  default void typecheckingSucceeded(Abstract.Definition definition) {}
  default void typecheckingFailed(Abstract.Definition definition) {}

  class Dummy implements TypecheckedReporter {}
}
