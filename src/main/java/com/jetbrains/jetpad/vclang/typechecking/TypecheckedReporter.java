package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Concrete;

public interface TypecheckedReporter {
  default void typecheckingSucceeded(Concrete.Definition definition) {}
  default void typecheckingFailed(Concrete.Definition definition) {}

  class Dummy implements TypecheckedReporter {}
}
