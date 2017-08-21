package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Concrete;

public interface TypecheckedReporter<T> {
  default void typecheckingSucceeded(Concrete.Definition<T> definition) {}
  default void typecheckingFailed(Concrete.Definition<T> definition) {}

  class Dummy<T> implements TypecheckedReporter<T> {}
}
